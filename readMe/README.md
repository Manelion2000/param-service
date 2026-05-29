# Reconciliation Service

Backend Spring Boot 3.5.x pour la reconciliation des transactions Banque vers Moov Money.

## 1. Vue d'ensemble

Le service permet de :
- importer des fichiers Banque et Moov (`CSV`, `XLS`, `XLSX`),
- parser, normaliser et stocker les transactions,
- lancer des executions de reconciliation par date metier,
- consulter les resultats (matches, anomalies, absences, doublons, ecarts),
- exporter un resultat en CSV.

## 2. Stack technique

- Java 21 (build cible projet)
- Spring Boot 3.5.11
- Spring Web, Spring Data JPA, Validation, Security
- PostgreSQL
- Flyway
- Apache POI (XLS/XLSX)
- Apache Commons CSV
- springdoc-openapi (Swagger)
- Maven

## 3. Arborescence fonctionnelle

Packages principaux utilises :
- `com.bakouan.app.controller` : endpoints REST
- `com.bakouan.app.service` / `service.impl` : logique metier
- `com.bakouan.app.service.parser` : parseurs fichiers
- `com.bakouan.app.model` : entites JPA
- `com.bakouan.app.repositories` : repositories JPA
- `com.bakouan.app.enums` : enums metier
- `com.bakouan.app.config` : CORS, securite, config globale
- `com.bakouan.app.utils` : utilitaires parsing/date/montant

## 4. Modele de donnees (reconciliation)

Tables creees par Flyway :
- `file_import`
- `bank_transaction`
- `moov_transaction`
- `reconciliation_run`
- `reconciliation_result`

Migration :
- `src/main/resources/db/migration/V1__init_reconciliation_tables.sql`

## 5. Enums metier

- `SourceType` : `BANQUE`, `MOOV`
- `ImportStatus` : `PENDING`, `SUCCESS`, `FAILED`, `PARTIAL_SUCCESS`
- `NormalizedBankStatus` : `SUCCESS_BANK`, `FAILED_BANK`, `UNKNOWN_BANK`
- `NormalizedMoovStatus` : `SUCCESS_MOOV`, `FAILED_MOOV`, `UNKNOWN_MOOV`
- `ReconciliationRunStatus` : `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`
- `ReconciliationResultType` :
  - `MATCH_OK`
  - `DEBIT_A_TORT`
  - `CREDIT_SANS_DEBIT`
  - `ECHEC_DES_DEUX_COTES`
  - `ABSENT_COTE_MOOV`
  - `ABSENT_COTE_BANQUE`
  - `MONTANT_DIFFERENT`
  - `DOUBLON_BANQUE`
  - `DOUBLON_MOOV`
  - `STATUT_INCONNU`

## 6. Parsing et validation fichiers

### 6.1 Formats supportes
- `.csv`
- `.xls`
- `.xlsx`

### 6.2 Validation parseurs
- validation des colonnes obligatoires par source,
- suppression des lignes vides,
- detection automatique delimiteur CSV `;` / `,`,
- normalisation des en-tetes et des valeurs,
- erreurs metier propres (`ParserValidationException`) avec message clair.

### 6.3 Colonnes obligatoires
- Banque : `ID transaction`
- Moov : `Receipt No.` (ou `Receipt No`) et `Transaction Status`

### 6.4 Mapping terrain implemente
Banque :
- cle : `ID transaction`
- statut : `Statut Allocation` ou `Status`
- msisdn : `MSISDN`, `Telephone`, `Téléphone`
- montant : `Montant` ou `Montant nominal`
- date : `Date transaction` ou `Date Operation`

Moov :
- cle : `Receipt No.`
- statut : `Transaction Status`
- msisdn : `Initiator MSISDN`
- montant : `Withdrawn` ou `Amount` (valeur absolue)
- dates : `Initiation Time`, `Completion Time`

## 7. Regles de reconciliation

Classification appliquee selon statuts normalises, presence/absence et montant :
- `MATCH_OK`
- `DEBIT_A_TORT`
- `CREDIT_SANS_DEBIT`
- `ECHEC_DES_DEUX_COTES`
- `ABSENT_COTE_MOOV`
- `ABSENT_COTE_BANQUE`
- `MONTANT_DIFFERENT` (avec tolerance `app.reconciliation.amount-tolerance`)
- `DOUBLON_BANQUE`
- `DOUBLON_MOOV`
- `STATUT_INCONNU`

## 8. API REST

Base URL : `http://localhost:8083`
Swagger : `http://localhost:8083/swagger-ui.html`
OpenAPI JSON : `http://localhost:8083/v3/api-docs`

### 8.1 Imports
- `POST /api/imports/bank` (multipart: `file`, `businessDate=YYYY-MM-DD`)
- `POST /api/imports/moov` (multipart: `file`, `businessDate=YYYY-MM-DD`)
- `GET /api/imports`
- `GET /api/imports/{id}`
- `GET /api/imports/{id}/errors`

### 8.2 Transactions
- `GET /api/transactions/bank?importId={id}`
- `GET /api/transactions/moov?importId={id}`

### 8.3 Reconciliation
- `POST /api/reconciliations/run`
- `GET /api/reconciliations/runs`
- `GET /api/reconciliations/runs/{id}`
- `GET /api/reconciliations/runs/{id}/results`
- `GET /api/reconciliations/runs/{id}/summary`

Vues metier :
- `GET /api/reconciliations/runs/{id}/matches`
- `GET /api/reconciliations/runs/{id}/debits-a-tort`
- `GET /api/reconciliations/runs/{id}/credits-sans-debit`
- `GET /api/reconciliations/runs/{id}/echecs`
- `GET /api/reconciliations/runs/{id}/absents-banque`
- `GET /api/reconciliations/runs/{id}/absents-moov`
- `GET /api/reconciliations/runs/{id}/doublons`
- `GET /api/reconciliations/runs/{id}/montants-differents`

Historique :
- `GET /api/reconciliations/history/by-date?businessDate=YYYY-MM-DD`
- `GET /api/reconciliations/history/range?dateFrom=YYYY-MM-DD&dateTo=YYYY-MM-DD`
- `GET /api/reconciliations/history/latest`

Export :
- `GET /api/reconciliations/runs/{id}/export/csv`
- `GET /api/reconciliations/runs/{id}/export/xlsx` (placeholder actuellement)

## 9. Exemples de payload

### 9.1 Lancer une reconciliation par date
```json
{
  "label": "RUN-2026-03-09",
  "businessDate": "2026-03-03"
}
```

### 9.2 Lancer une reconciliation par plage
```json
{
  "label": "RUN-RANGE-2026-03",
  "dateFrom": "2026-03-01",
  "dateTo": "2026-03-05"
}
```

## 10. Configuration

### 10.1 Fichiers
- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-test.yml`
- `src/main/resources/application-prod.yml`

### 10.2 Proprietes importantes
- `app.storage.path` : dossier stockage uploads
- `app.cors.allowed-origins` : CORS front
- `app.reconciliation.amount-tolerance` : tolerance ecart montant

## 11. Execution locale

Prerequis :
- JDK 21 recommande (le projet cible Java 21)
- Maven 3.9+
- PostgreSQL

Commandes :
```bash
mvn clean spring-boot:run -Dspring-boot.run.profiles=dev
```

## 12. Docker

```bash
docker compose up --build
```

Fichiers :
- `Dockerfile`
- `docker-compose.yml`

## 13. Tests

- Unitaires : normalisation statuts, classification
- Integration : import reels + execution reconciliation

Commande :
```bash
mvn test
```

Test d'integration cible :
- `com.bakouan.app.service.ReconciliationSampleFilesIT`

## 14. Jeux de fichiers d'exemple

Dossier :
- `src/main/resources/fichier`

Fichiers utilises pour validation :
- `Moov Money - BSIC Daily Reconciliation_2026-03-04.xls`
- `operation_carthago_jasper.xls`

## 15. Limitations connues / backlog

- endpoint `export/xlsx` a finaliser (POI workbook)
- filtrage transactions peut etre enrichi (status/date/montant/msisdn multi-criteres)
- durcir la validation metier (types, longueurs, coherence date)
- securite JWT a consolider selon profil prod cible

## 16. Commandes utiles

- Compilation rapide :
```bash
mvn -DskipTests compile
```

- Test cible integration :
```bash
mvn -Dtest=ReconciliationSampleFilesIT test
```
