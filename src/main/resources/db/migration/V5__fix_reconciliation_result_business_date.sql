UPDATE reconciliation_result rr
SET business_date = COALESCE(
    (
        SELECT fi.business_date
        FROM bank_transaction bt
        JOIN file_import fi ON fi.id = bt.import_id
        WHERE bt.id = rr.bank_transaction_id
    ),
    (
        SELECT fi.business_date
        FROM moov_transaction mt
        JOIN file_import fi ON fi.id = mt.import_id
        WHERE mt.id = rr.moov_transaction_id
    ),
    (
        SELECT r.business_date_from
        FROM reconciliation_run r
        WHERE r.id = rr.run_id
    )
)
WHERE COALESCE(
    (
        SELECT fi.business_date
        FROM bank_transaction bt
        JOIN file_import fi ON fi.id = bt.import_id
        WHERE bt.id = rr.bank_transaction_id
    ),
    (
        SELECT fi.business_date
        FROM moov_transaction mt
        JOIN file_import fi ON fi.id = mt.import_id
        WHERE mt.id = rr.moov_transaction_id
    ),
    (
        SELECT r.business_date_from
        FROM reconciliation_run r
        WHERE r.id = rr.run_id
    )
) IS DISTINCT FROM rr.business_date;
