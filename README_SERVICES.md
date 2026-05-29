# Documentation des services

Ce document explique les services metier du projet `reconcilliation-service` et leurs interactions.

## 1. Vue globale des services

Le package `com.bakouan.app.service` contient :
- des services applicatifs (import, reconciliation, retention),
- des services techniques (stockage, mail, logs, reporting),
- des services de support metier (normalisation, classification),
- des interfaces et leurs implementations (`service.impl`),
- des strategies de reconciliation (`service.reconciliation`),
- des parseurs de fichiers (`service.parser`).

## 2. Services coeur reconciliation

### `ReconciliationService` (interface)
Responsabilites :
- lancer une execution de reconciliation (`run`),
- consulter les runs et resultats,
- fournir des resumes (`summary`, `globalSummary`),
- filtrer les resultats globalement par dates/type.

### `ReconciliationServiceImpl`
Role principal du workflow de reconciliation.

Fonctionnement :
- determine la plage date metier a partir de `ReconciliationRunRequest`,
- recupere les imports Banque correspondants,
- cree un `ReconciliationRun` en statut `RUNNING`,
- charge les transactions Banque,
- delegue la reconciliation operateur a chaque `ReconciliationOperatorStrategy` (Moov, Orange),
- finalise le run (`COMPLETED`), calcule et stocke le resume.

Points importants :
- supporte BANQUE vs MOOV et BANQUE vs ORANGE via strategie,
- calcule les KPI (taux, totaux montants, categories d'erreurs),
- gere le cas `ABSENT_COTE_ORANGE` comme equivalent fonctionnel d'absence cote operateur dans certains filtres.

### `ReconciliationClassificationService`
Service de regles metier qui classe une paire de transactions en `ReconciliationResultType`.

Regles principales :
- gestion des absences (banque / operateur),
- gestion des statuts inconnus,
- controle ecart montant avec tolerance `app.reconciliation.amount-tolerance`,
- mapping des combinaisons statut banque + statut operateur vers :
  `MATCH_OK`, `DEBIT_A_TORT`, `CREDIT_SANS_DEBIT`, `ECHEC_DES_DEUX_COTES`, etc.

Il expose deux surcharges :
- `classify(BankTransaction, MoovTransaction)`,
- `classify(BankTransaction, OrangeTransaction)`.

### `ReconciliationResultWriter`
Service utilitaire pour persister une ligne `ReconciliationResult`.

Responsabilites :
- construire un resultat coherent (ids, statuts bruts, montants, difference, raison),
- determiner `businessDate` a partir des imports disponibles,
- sauvegarder via `ReconciliationResultRepository`.

## 3. Strategies de reconciliation operateur

### `ReconciliationOperatorStrategy` (interface)
Contrat commun des strategies :
- `operatorSourceType()`,
- `findImports(from, to)`,
- `reconcile(run, bankRows, operatorImports)`.

### `MoovReconciliationOperatorStrategy`
Implementation pour source `MOOV`.

Responsabilites :
- charger imports et transactions Moov sur la plage,
- grouper Banque (`transactionId`) et Moov (`receiptNo`) par cle de rapprochement,
- detecter doublons banque/moov,
- classer chaque paire via `ReconciliationClassificationService`,
- ecrire le resultat via `ReconciliationResultWriter`.

### `OrangeReconciliationOperatorStrategy`
Implementation pour source `ORANGE`.

Responsabilites :
- meme logique que Moov mais sur `omTransactionId`,
- detection doublons banque / orange,
- classification Banque-Orange puis persistence des resultats.

Note :
- la sauvegarde reutilise les colonnes `moov_*` dans `ReconciliationResult` pour les donnees Orange aussi (choix de schema commun).

## 4. Services d'import et parsing

### `FileImportService` (interface)
Expose les operations metier d'import :
- import d'un fichier (`importFile`),
- consultation (`list`, `get`),
- suppression du dernier import d'une source,
- suppression en masse par source + date metier,
- previsualisation d'impact avant suppression.

### `FileImportServiceImpl`
Service central d'ingestion des fichiers.

Pipeline `importFile` :
- choisit le parseur compatible (`TransactionFileParser`) selon extension,
- stocke physiquement le fichier via `FileStorageService`,
- cree un enregistrement `FileImport` (`PENDING`),
- parse le contenu,
- dedoublonne (intra-fichier + deja en base),
- mappe chaque ligne vers entite transaction (`BankTransaction`, `MoovTransaction`, `OrangeTransaction`),
- normalise les statuts via `StatusNormalizationService`,
- met a jour les compteurs (`totalRows`, `validRows`, `invalidRows`) et le statut final.

Suppression d'imports :
- supprime transactions associees,
- supprime resultats/runs de reconciliation impactes,
- supprime l'import et le fichier physique,
- renvoie un bilan detaille (`ImportDeletionResult`, `ImportBulkDeletionResult`, `ImportDeletionPreviewResult`).

### `StatusNormalizationService`
Normalise les statuts bruts fichiers vers enums internes :
- banque -> `NormalizedBankStatus`,
- moov -> `NormalizedMoovStatus`,
- orange -> `NormalizedOrangeStatus`.

Utilite :
- reduire la variabilite des libelles source,
- fiabiliser les regles de classification.

### `TransactionFileParser` (interface)
Contrat parseur :
- `supports(filename)` pour selection,
- `parse(file, sourceType)` pour extraction normalisee des lignes.

### `CsvTransactionFileParser`
Parse les CSV :
- detecte delimiteur (`;` ou `,`),
- lit headers et valide colonnes obligatoires,
- normalise les lignes et ignore les lignes vides.

### `XlsTransactionFileParser`
Parse les `.xls` (et logique commune Excel) :
- recherche dynamique de la ligne d'entete,
- validation headers obligatoires,
- normalisation des lignes,
- ajout `_line_number` pour tracer les erreurs ligne source.

### `XlsxTransactionFileParser`
Delegue la lecture a la logique Excel commune du parseur XLS.

### `ParserSupport` (utilitaire package parser)
Fonctions de support :
- normalisation header,
- verification lignes vides,
- validation colonnes obligatoires par `SourceType`,
- normalisation des maps de lignes.

### Exceptions parsing/import
- `ParserValidationException` : erreur metier de parsing (colonnes manquantes, format invalide).
- `ReconciliationException` : exception metier generique (ex: suppression d'import impossible).

## 5. Services retention

### `ReconciliationRetentionService` (interface)
Expose les operations de retention :
- execution avec cutoff explicite,
- execution avec cutoff calcule automatiquement,
- surcharge avec mode de retention force.

### `ReconciliationRetentionServiceImpl`
Gestion du cycle de vie des donnees de reconciliation.

Responsabilites :
- calcule cutoff par defaut (`now - keepDays`),
- execute la retention en batch transactionnel,
- modes supportes :
  - `ARCHIVE_AND_PURGE` : archive puis purge,
  - mode purge direct (selon `RetentionProperties`),
- orchestre la purge des resultats puis runs en respectant les dependances.

Execution planifiee :
- methode `scheduledRetention()` avec cron `app.retention.cron` (defaut `0 30 2 * * *`),
- ne s'execute que si `app.retention.enabled=true`.

## 6. Services techniques et legacy/backoffice

### `FileStorageService` (interface)
Abstraction stockage fichier :
- `store(MultipartFile)` -> chemin du fichier stocke,
- `deleteIfExists(path)` -> suppression silencieuse.

### `LocalFileStorageService`
Implementation locale filesystem :
- dossier racine `app.storage.path` (defaut `./datas`),
- nommage par UUID,
- suppression physique non bloquante (n'interrompt pas la suppression metier en base).

### `BaFileStorageService`
Service de stockage binaire historique :
- `save`, `get`, `update`, `remove` sur filesystem,
- utilise `app.storage.path`.

### `BaLogService`
Service de journalisation applicative :
- mappe `BaLogDto` vers `BaLog`,
- enrichit avec IP client via `HttpServletRequest`,
- persiste dans `BaLogRepository`.

### `BaMailService`
Service d'envoi de mails asynchrones :
- envoi direct (`sendEmail`),
- envoi via template Thymeleaf (`sendEmailFromTemplate`, `sendMessage`),
- tentative d'archivage du mail envoye dans le dossier IMAP `Sent`.

### `BaParamService` / `BaParamServiceImpl`
Services de gestion parametres metier (categories/produits).

Fonctions :
- CRUD categories et produits,
- suppression logique de categorie (`statut D`),
- journalisation des actions via `BaLogService`.

### `BaReportService`
Service de generation de rapports Jasper :
- charge template `.jasper`,
- convertit DTO en datasource JSON,
- genere un binaire (PDF notamment),
- exemple present : impression d'une liste de produits.

## 7. Interactions principales entre services

Flux import :
1. `FileImportServiceImpl` choisit un `TransactionFileParser`.
2. Le parseur produit des lignes normalisees.
3. `StatusNormalizationService` standardise les statuts.
4. Les transactions sont persistees et liees a `FileImport`.

Flux reconciliation :
1. `ReconciliationServiceImpl` cree un `ReconciliationRun`.
2. Chaque `ReconciliationOperatorStrategy` traite ses imports operateur.
3. `ReconciliationClassificationService` classe les paires.
4. `ReconciliationResultWriter` persiste chaque resultat.
5. `ReconciliationServiceImpl` calcule le resume final.

Flux retention :
1. `ReconciliationRetentionServiceImpl` calcule cutoff/mode.
2. Archive (optionnel) puis purge `reconciliation_result`.
3. Purge `reconciliation_run` orphelins selon regles.

## 8. Ou trouver le code

- Services interfaces/classes :
  `src/main/java/com/bakouan/app/service`
- Implementations :
  `src/main/java/com/bakouan/app/service/impl`
- Strategies reconciliation :
  `src/main/java/com/bakouan/app/service/reconciliation`
- Parseurs :
  `src/main/java/com/bakouan/app/service/parser`
