# Cahier de description des fonctionnalites

## 1. Objectif de la plateforme

La plateforme de reconciliation monetique permet de comparer les transactions issues de la Banque avec les transactions des operateurs Mobile Money, principalement MOOV Money et Orange Money.

Elle centralise les imports de fichiers, execute les rapprochements, classe les ecarts, produit des indicateurs de pilotage, controle la comptabilisation AMPLITUDE et fournit des etats de compensation.

Les objectifs principaux sont :
- securiser le controle des flux Banque vers Wallet et Wallet vers Banque ;
- detecter rapidement les anomalies entre la Banque et les operateurs ;
- faciliter les travaux quotidiens de justification et de compensation ;
- fournir une vision consolidee par jour, periode et canal ;
- conserver une trace des imports, traitements, resultats et rapports.

## 2. Perimetre fonctionnel

La plateforme couvre les domaines suivants :
- Import des fichiers Banque, MOOV, Orange et AMPLITUDE ;
- Reconciliation Banque / MOOV ;
- Reconciliation Banque / Orange ;
- Consultation des resultats et des anomalies ;
- Tableaux de bord de suivi ;
- Reporting et exports ;
- Controle de comptabilisation AMPLITUDE ;
- Compensation journaliere, hebdomadaire et mensuelle ;
- Nettoyage et retention des donnees ;
- Administration des utilisateurs, roles, profils et parametres.

## 3. Profils utilisateurs

### 3.1 Agent de reconciliation

L'agent de reconciliation importe les fichiers sources, lance les traitements de rapprochement et consulte les anomalies.

Il intervient principalement sur :
- le chargement des fichiers Banque et operateur ;
- le lancement d'une reconciliation ;
- la verification des lignes en ecart ;
- l'export des resultats pour analyse.

### 3.2 Agent comptable

L'agent comptable controle que les operations Banque attendues sont bien comptabilisees dans AMPLITUDE.

Il intervient principalement sur :
- l'import du fichier AMPLITUDE ;
- la consultation des transactions comptabilisees et non comptabilisees ;
- le suivi des montants a risque ;
- l'export des KPI comptables.

### 3.3 Responsable operationnel

Le responsable operationnel suit la performance globale des rapprochements et valide les positions de compensation.

Il intervient principalement sur :
- les tableaux de bord ;
- les taux de succes et d'anomalie ;
- la compensation par jour, semaine ou mois ;
- l'analyse des anomalies majeures.

### 3.4 Administrateur

L'administrateur gere les acces, les profils et les parametres fonctionnels.

Il intervient principalement sur :
- les utilisateurs ;
- les roles ;
- les profils ;
- les categories et produits ;
- les operations de nettoyage ou retention, lorsque celles-ci sont autorisees.

## 4. Parcours utilisateur principal

Le parcours standard de reconciliation est le suivant :

1. Choisir le canal de travail : MOOV ou ORANGE.
2. Importer le fichier Banque correspondant au canal.
3. Importer le fichier operateur correspondant.
4. Selectionner la date metier ou la periode.
5. Lancer la reconciliation.
6. Consulter le resume du traitement.
7. Analyser les resultats par categorie d'anomalie.
8. Exporter les resultats si necessaire.
9. Suivre les indicateurs dans le tableau de bord.
10. Exploiter les donnees de compensation.

Pour le controle comptable :

1. Importer le fichier AMPLITUDE.
2. Selectionner le canal et la periode.
3. Lancer ou consulter le controle de comptabilisation.
4. Identifier les operations comptabilisees, non comptabilisees et a risque.
5. Exporter les KPI comptables si necessaire.

## 5. Module d'import de fichiers

### 5.1 Role du module

Le module d'import permet de charger les fichiers necessaires aux traitements.

Chaque fichier importe est historise avec :
- le type de source ;
- le canal operateur lorsque necessaire ;
- le nom du fichier original ;
- la date metier ;
- le nombre de lignes lues ;
- le nombre de lignes valides ;
- le nombre de lignes invalides ;
- le statut de l'import ;
- les erreurs eventuelles.

### 5.2 Sources supportees

Les sources supportees sont :
- BANQUE ;
- MOOV ;
- ORANGE ;
- AMPLITUDE.

### 5.3 Formats supportes

La plateforme prend en charge les fichiers CSV, XLS et XLSX selon les sources.

Les fichiers Excel sont lus avec detection de la ligne d'entete lorsque les premieres lignes contiennent des titres, des commentaires ou des informations de rapport.

### 5.4 Import Banque

L'import Banque represente le cote bancaire des operations.

Il supporte notamment :
- les anciens fichiers Banque classiques ;
- les exports Carthago Bank to Wallet ;
- les exports Carthago Wallet to Bank ;
- les fichiers rattaches au canal MOOV ;
- les fichiers rattaches au canal Orange.

Les champs importants sont :
- identifiant transaction ;
- reference operation ;
- numero de telephone ;
- montant ;
- statut d'allocation ou de desallocation ;
- nature de l'operation ;
- date transaction.

### 5.5 Import MOOV

L'import MOOV represente le cote operateur MOOV Money.

La plateforme distingue deux directions :
- Banque vers Wallet : le montant est extrait de la colonne Withdrawn ;
- Wallet vers Banque : le montant est extrait de la colonne Paid In.

La colonne Details est utilisee pour reconnaitre la nature de l'operation :
- Transfer from Bank to Moov Money ;
- Transfer from Moov Money to Bank.

### 5.6 Import Orange

L'import Orange represente le cote operateur Orange Money.

Pour les fichiers Banque issus de Carthago et rattaches au canal Orange, le fichier doit etre importe comme source BANQUE avec le canal ORANGE.

Les exports Carthago Orange Bank to Wallet et Wallet to Bank sont traites via la nature d'operation :
- BankToWallet ;
- WalletToBank.

### 5.7 Import AMPLITUDE

L'import AMPLITUDE alimente le controle de comptabilisation.

La plateforme traite les colonnes de montants avec la logique suivante :
- le credit est prioritaire lorsqu'il est renseigne ;
- le debit est utilise lorsque le credit est vide ;
- cette logique permet de couvrir les operations Banque vers Wallet et Wallet vers Banque.

## 6. Module de reconciliation

### 6.1 Role du module

Le module de reconciliation compare les transactions Banque avec les transactions operateur sur une date ou une periode.

Il cree une execution appelee run de reconciliation. Chaque run contient :
- un libelle ;
- un canal ;
- une date ou periode ;
- les imports utilises ;
- un statut d'execution ;
- un resume ;
- une liste de resultats detailles.

### 6.2 Canaux supportes

Les canaux supportes sont :
- MOOV ;
- ORANGE.

### 6.3 Cles de rapprochement

La reconciliation rapproche les lignes a partir des references transactionnelles normalisees.

Selon la source, la cle peut provenir :
- de l'identifiant transaction Banque ;
- du receipt number MOOV ;
- de l'identifiant transaction Orange ;
- de la reference operation lorsque le format source l'impose.

### 6.4 Directions d'operation

La plateforme classe les operations selon deux directions generiques :
- BANK_TO_WALLET : Banque vers Wallet ;
- WALLET_TO_BANK : Wallet vers Banque.

Les anciennes valeurs specifiques MOOV restent compatibles :
- BANK_TO_MOOV ;
- MOOV_TO_BANK.

### 6.5 Categories de resultats

Les resultats de reconciliation sont classes comme suit :

- MATCH_OK : la transaction est coherente des deux cotes.
- DEBIT_A_TORT : la Banque est en succes alors que l'operateur est en echec ou absent.
- CREDIT_SANS_DEBIT : l'operateur est en succes alors que la Banque est en echec ou absente.
- ECHEC_DES_DEUX_COTES : les deux cotes sont en echec.
- ABSENT_COTE_BANQUE : la transaction existe uniquement cote operateur.
- ABSENT_COTE_MOOV : la transaction existe uniquement cote Banque pour le canal MOOV.
- ABSENT_COTE_ORANGE : la transaction existe uniquement cote Banque pour le canal Orange.
- MONTANT_DIFFERENT : les deux transactions existent mais les montants sont differents au-dela de la tolerance.
- DOUBLON_BANQUE : plusieurs transactions Banque portent la meme cle.
- DOUBLON_MOOV : plusieurs transactions operateur portent la meme cle. Le libelle historique est conserve aussi pour Orange.
- STATUT_INCONNU : un statut source ne peut pas etre normalise.

### 6.6 Filtres disponibles

L'utilisateur peut filtrer les resultats par :
- categorie de resultat ;
- direction d'operation ;
- date ou periode ;
- cle transactionnelle ;
- numero de telephone ;
- statut ;
- montant ;
- canal.

### 6.7 Exports

Les resultats d'un run peuvent etre exportes en :
- CSV ;
- XLSX.

## 7. Module tableau de bord

### 7.1 Role du module

Le tableau de bord donne une vue de pilotage sur les reconciliations.

Il permet de suivre :
- le volume de transactions ;
- le nombre de transactions rapprochees ;
- le nombre d'anomalies ;
- les taux de succes ;
- les montants Banque et operateur ;
- les ecarts globaux ;
- la qualite des imports.

### 7.2 Indicateurs principaux

Les indicateurs affiches incluent :
- total Banque ;
- total operateur ;
- total des resultats ;
- nombre de MATCH_OK ;
- nombre de DEBIT_A_TORT ;
- nombre de CREDIT_SANS_DEBIT ;
- nombre de ECHEC_DES_DEUX_COTES ;
- absences cote operateur ;
- absences cote Banque ;
- montants differents ;
- statuts inconnus ;
- doublons ;
- taux de rapprochement ;
- taux de succes ;
- taux d'anomalie.

### 7.3 Analyses disponibles

Le tableau de bord permet aussi de consulter :
- la distribution des resultats ;
- l'evolution des volumes dans le temps ;
- les principales anomalies ;
- la qualite des donnees importees.

## 8. Module reporting

### 8.1 Role du module

Le module reporting produit des syntheses periodiques sur les reconciliations.

Il permet de choisir :
- le canal ;
- le type de periode ;
- la date de reference.

### 8.2 Periodes supportees

Les periodes supportees sont :
- journalier ;
- hebdomadaire ;
- mensuel.

### 8.3 Contenu du reporting

Le reporting contient :
- les KPI de la periode ;
- la repartition des resultats ;
- le detail journalier ;
- la liste des transactions significatives ;
- les montants Banque, operateur et anomalies.

### 8.4 Exports reporting

Les rapports peuvent etre exportes en :
- Excel ;
- PDF.

## 9. Module comptabilisation AMPLITUDE

### 9.1 Role du module

Le module de comptabilisation controle que les operations Banque attendues sont retrouvees dans AMPLITUDE.

Il aide a identifier :
- les transactions comptabilisees ;
- les transactions non comptabilisees ;
- les montants a risque ;
- les ecarts de montant.

### 9.2 Perimetre controle

Seules les operations Banque considerees comme candidates sont controlees.

Les operations allouees sont attendues dans AMPLITUDE.

Les operations rejetees, non allouees ou aux statuts inconnus sont exclues du perimetre comptable selon les regles metier.

### 9.3 Statuts comptables

Les statuts affiches sont :
- COMPTABILISE : la transaction Banque est retrouvee dans AMPLITUDE ;
- NON_COMPTABILISE : la transaction Banque attendue n'est pas retrouvee dans AMPLITUDE.

### 9.4 Filtres disponibles

L'utilisateur peut filtrer les lignes comptables par :
- canal ;
- date ou periode ;
- statut comptable ;
- numero de telephone ;
- numero de compte ;
- reference operation ;
- identifiant transaction.

### 9.5 KPI comptables

Les KPI comptables incluent :
- nombre total de transactions candidates ;
- montant total controle ;
- nombre et montant comptabilises ;
- nombre et montant non comptabilises ;
- taux de comptabilisation ;
- montant a risque ;
- ecart net ;
- ecart absolu ;
- nombre de transactions avec ecart.

### 9.6 Exports comptables

Les KPI comptables peuvent etre exportes en :
- CSV ;
- PDF.

## 10. Module compensation

### 10.1 Role du module

Le module de compensation compare les positions Banque et operateur pour aider a la decision de compensation.

Il permet de suivre les differences de montants sur une journee, une semaine ou un mois.

### 10.2 Vues disponibles

Les vues supportees sont :
- compensation journaliere ;
- compensation hebdomadaire ;
- compensation mensuelle ;
- detail des ecarts.

### 10.3 Indicateurs de compensation

Les indicateurs affiches incluent :
- nombre de succes operateur ;
- nombre de succes Banque ;
- montant succes operateur ;
- montant succes Banque ;
- difference ;
- decision.

### 10.4 Decisions

La plateforme peut afficher une decision de synthese :
- OK_COMPENSATION : les donnees sont coherentes pour compensation ;
- A_VERIFIER : des ecarts ou risques necessitent une verification.

### 10.5 Analyse des risques

Les ecarts peuvent etre filtres par :
- absent cote operateur ;
- absent cote Banque ;
- echec des deux cotes.

## 11. Module retention et nettoyage

### 11.1 Retention des reconciliations

La retention permet de purger ou archiver les anciennes donnees de reconciliation selon une date limite ou un nombre de jours de conservation.

Modes disponibles :
- archive puis purge ;
- purge seule.

### 11.2 Nettoyage des imports

La plateforme permet :
- de supprimer le dernier import d'une source ;
- de supprimer les imports d'une source pour une date metier ;
- de supprimer tous les imports d'une source ;
- de previsualiser l'impact avant suppression.

La previsualisation indique :
- le nombre d'imports candidats ;
- le nombre de transactions concernees ;
- le nombre de resultats impactes ;
- le nombre de runs impactes.

### 11.3 Nettoyage AMPLITUDE

Les donnees AMPLITUDE peuvent etre nettoyees selon :
- une date ;
- une periode ;
- une semaine ;
- toutes les donnees.

## 12. Module administration

### 12.1 Utilisateurs

L'administration permet de :
- consulter les utilisateurs ;
- creer un utilisateur ;
- modifier un utilisateur ;
- supprimer un utilisateur ;
- activer un utilisateur ;
- changer un mot de passe ;
- demander ou finaliser une reinitialisation de mot de passe.

### 12.2 Roles et profils

L'administration permet de :
- consulter les roles ;
- creer un role ;
- modifier un role ;
- supprimer un role ;
- consulter les profils ;
- creer un profil ;
- modifier un profil ;
- supprimer un profil.

### 12.3 Parametres metier

Les parametres metier disponibles incluent :
- categories ;
- produits.

Chaque categorie ou produit peut etre cree, consulte, modifie ou supprime selon les droits de l'utilisateur.

## 13. Regles de controle des fichiers

### 13.1 Controle des lignes

Lors de l'import, la plateforme :
- ignore les lignes vides ;
- detecte les entetes ;
- controle les colonnes obligatoires ;
- normalise les statuts ;
- normalise les montants ;
- detecte les doublons ;
- compte les lignes invalides.

### 13.2 Gestion des erreurs

Lorsqu'une ligne est invalide, elle est rejetee et comptee dans les lignes invalides.

Lorsqu'un fichier ne respecte pas le format attendu, l'import passe en erreur et le message est conserve.

### 13.3 Doublons

Les doublons sont controles :
- pendant l'import ;
- pendant la reconciliation.

Un doublon peut etre detecte lorsque plusieurs lignes portent la meme cle transactionnelle.

## 14. Bonnes pratiques d'utilisation

Avant de lancer une reconciliation :
- verifier que le bon canal est selectionne ;
- importer le fichier Banque du canal concerne ;
- importer le fichier operateur correspondant ;
- verifier la date metier ;
- s'assurer que les fichiers ne sont pas deja importes.

Pendant l'analyse :
- commencer par les categories a fort impact financier ;
- verifier les montants differents ;
- analyser les absences cote Banque ou cote operateur ;
- controler les statuts inconnus ;
- exporter les resultats si une analyse externe est necessaire.

Pour la comptabilisation :
- importer AMPLITUDE avant de consulter le controle comptable ;
- verifier les operations non comptabilisees ;
- suivre le montant a risque ;
- exporter les KPI pour justification.

Pour la compensation :
- consulter d'abord la vue journaliere ;
- utiliser la vue hebdomadaire ou mensuelle pour la consolidation ;
- verifier les lignes marquees A_VERIFIER avant toute decision finale.

## 15. Glossaire

- Banque : donnees issues du systeme bancaire ou de Carthago.
- Wallet : compte Mobile Money du client.
- Operateur : MOOV Money ou Orange Money.
- Bank to Wallet : operation de la Banque vers le Wallet.
- Wallet to Bank : operation du Wallet vers la Banque.
- Reconciliation : comparaison entre les donnees Banque et les donnees operateur.
- Run : execution d'un traitement de reconciliation.
- AMPLITUDE : systeme comptable utilise pour verifier la comptabilisation.
- Compensation : synthese des positions financieres entre Banque et operateur.
- Match OK : transaction coherente des deux cotes.
- Anomalie : difference, absence, echec ou doublon detecte pendant le rapprochement.

## 16. Resultat attendu pour les utilisateurs

En utilisant correctement la plateforme, les equipes doivent pouvoir :
- importer les fichiers quotidiens ;
- produire un rapprochement fiable par canal ;
- identifier les anomalies a traiter ;
- justifier les ecarts ;
- verifier la comptabilisation AMPLITUDE ;
- suivre les positions de compensation ;
- produire des exports pour reporting et controle interne.
