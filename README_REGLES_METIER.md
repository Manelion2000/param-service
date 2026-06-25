# Regles metier reconciliation et comptabilisation

## Reconciliation Banque / Operateur

Le rapprochement compare les transactions Banque avec les transactions operateur du canal selectionne (`MOOV` ou `ORANGE`).

### Perimetre
- Banque: imports `BANQUE` rattaches a l'operateur du run.
- Operateur: imports `MOOV` ou `ORANGE` sur la periode du run.
- La date metier du resultat vient en priorite de la date de transaction reelle, puis de la date d'import, puis de la date du run.

### Classification
- `MATCH_OK`: les deux cotes existent, les statuts sont en succes, et les montants sont coherents.
- `DEBIT_A_TORT`: la Banque est en succes, l'operateur est en echec ou absent selon le canal.
- `CREDIT_SANS_DEBIT`: l'operateur est en succes, la Banque est en echec ou absente.
- `ECHEC_DES_DEUX_COTES`: Banque et operateur sont en echec.
- `ABSENT_COTE_BANQUE`: la transaction existe cote operateur seulement.
- `ABSENT_COTE_MOOV` / `ABSENT_COTE_ORANGE`: la transaction existe cote Banque seulement.
- `MONTANT_DIFFERENT`: les deux cotes existent, mais l'ecart depasse la tolerance configuree.
- `DOUBLON_BANQUE`: plusieurs lignes Banque partagent la meme cle.
- `DOUBLON_MOOV`: plusieurs lignes operateur partagent la meme cle. Le nom historique reste `DOUBLON_MOOV`, meme pour Orange.
- `STATUT_INCONNU`: au moins un statut ne peut pas etre normalise.

### Reason codes
Les raisons stockees dans `reconciliation_result.reason` suivent le format:

```text
OPERATEUR:CODE
```

Exemples:
- `MOOV:MATCH_OK`
- `ORANGE:ABSENT_COTE_OPERATEUR`
- `MOOV:DOUBLON_BANQUE`

## Comptabilisation Carthago / AMPLITUDE

La comptabilisation controle les operations Banque candidates contre AMPLITUDE.

### Perimetre comptable
Seules les operations Banque normalisees `SUCCESS_BANK` sont candidates.

Regles detaillees:
- `Alloue` + present dans AMPLITUDE: `COMPTABILISE`.
- `Alloue` + absent dans AMPLITUDE: `NON_COMPTABILISE`, donc operation a risque.
- `Paiement genere` + present dans AMPLITUDE: `COMPTABILISE`.
- `Paiement genere` + absent dans AMPLITUDE: ecarte du perimetre, comme une operation non allouee.
- `Non alloue`, rejet, echec allocation ou statut inconnu: ecarte du perimetre comptable.

### Matching AMPLITUDE
Le matching se fait en priorite par reference operation normalisee.

Fallback autorise:
- date d'operation a plus ou moins un jour,
- montant identique,
- compte identique si les deux comptes sont disponibles.

### KPI
Les KPI comptables ne comptent que le perimetre comptable retenu apres exclusion.

- `totalTransactions`: operations candidates retenues.
- `comptabilizedTransactions`: candidates trouvees dans AMPLITUDE.
- `nonComptabilizedTransactions`: candidates allouees non trouvees dans AMPLITUDE.
- `amountAtRisk`: montant des candidates allouees non trouvees dans AMPLITUDE.
- Les paiements generes absents d'AMPLITUDE ne contribuent pas au risque.
