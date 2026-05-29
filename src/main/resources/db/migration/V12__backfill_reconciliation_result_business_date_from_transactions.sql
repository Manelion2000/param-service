UPDATE reconciliation_result rr
SET business_date = COALESCE(
    (
        SELECT CAST(bt.transaction_date AS DATE)
        FROM bank_transaction bt
        WHERE bt.id = rr.bank_transaction_id
    ),
    (
        SELECT CAST(mt.completion_time AS DATE)
        FROM moov_transaction mt
        WHERE mt.id = rr.moov_transaction_id
          AND (
              EXISTS (
                  SELECT 1
                  FROM reconciliation_run run
                  WHERE run.id = rr.run_id
                    AND run.operator = 'MOOV'
              )
              OR (
                  EXISTS (
                      SELECT 1
                      FROM reconciliation_run run
                      WHERE run.id = rr.run_id
                        AND run.operator IS NULL
                  )
                  AND rr.reason LIKE 'MOOV:%'
              )
          )
    ),
    (
        SELECT CAST(mt.initiation_time AS DATE)
        FROM moov_transaction mt
        WHERE mt.id = rr.moov_transaction_id
          AND (
              EXISTS (
                  SELECT 1
                  FROM reconciliation_run run
                  WHERE run.id = rr.run_id
                    AND run.operator = 'MOOV'
              )
              OR (
                  EXISTS (
                      SELECT 1
                      FROM reconciliation_run run
                      WHERE run.id = rr.run_id
                        AND run.operator IS NULL
                  )
                  AND rr.reason LIKE 'MOOV:%'
              )
          )
    ),
    (
        SELECT CAST(ot.transaction_date_time AS DATE)
        FROM orange_transaction ot
        WHERE ot.id = rr.moov_transaction_id
          AND (
              EXISTS (
                  SELECT 1
                  FROM reconciliation_run run
                  WHERE run.id = rr.run_id
                    AND run.operator = 'ORANGE'
              )
              OR (
                  EXISTS (
                      SELECT 1
                      FROM reconciliation_run run
                      WHERE run.id = rr.run_id
                        AND run.operator IS NULL
                  )
                  AND rr.reason LIKE 'ORANGE:%'
              )
          )
    ),
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
          AND (
              EXISTS (
                  SELECT 1
                  FROM reconciliation_run run
                  WHERE run.id = rr.run_id
                    AND run.operator = 'MOOV'
              )
              OR (
                  EXISTS (
                      SELECT 1
                      FROM reconciliation_run run
                      WHERE run.id = rr.run_id
                        AND run.operator IS NULL
                  )
                  AND rr.reason LIKE 'MOOV:%'
              )
          )
    ),
    (
        SELECT fi.business_date
        FROM orange_transaction ot
        JOIN file_import fi ON fi.id = ot.import_id
        WHERE ot.id = rr.moov_transaction_id
          AND (
              EXISTS (
                  SELECT 1
                  FROM reconciliation_run run
                  WHERE run.id = rr.run_id
                    AND run.operator = 'ORANGE'
              )
              OR (
                  EXISTS (
                      SELECT 1
                      FROM reconciliation_run run
                      WHERE run.id = rr.run_id
                        AND run.operator IS NULL
                  )
                  AND rr.reason LIKE 'ORANGE:%'
              )
          )
    ),
    (
        SELECT run.business_date_from
        FROM reconciliation_run run
        WHERE run.id = rr.run_id
    )
)
WHERE COALESCE(
    (
        SELECT CAST(bt.transaction_date AS DATE)
        FROM bank_transaction bt
        WHERE bt.id = rr.bank_transaction_id
    ),
    (
        SELECT CAST(mt.completion_time AS DATE)
        FROM moov_transaction mt
        WHERE mt.id = rr.moov_transaction_id
          AND (
              EXISTS (
                  SELECT 1
                  FROM reconciliation_run run
                  WHERE run.id = rr.run_id
                    AND run.operator = 'MOOV'
              )
              OR (
                  EXISTS (
                      SELECT 1
                      FROM reconciliation_run run
                      WHERE run.id = rr.run_id
                        AND run.operator IS NULL
                  )
                  AND rr.reason LIKE 'MOOV:%'
              )
          )
    ),
    (
        SELECT CAST(mt.initiation_time AS DATE)
        FROM moov_transaction mt
        WHERE mt.id = rr.moov_transaction_id
          AND (
              EXISTS (
                  SELECT 1
                  FROM reconciliation_run run
                  WHERE run.id = rr.run_id
                    AND run.operator = 'MOOV'
              )
              OR (
                  EXISTS (
                      SELECT 1
                      FROM reconciliation_run run
                      WHERE run.id = rr.run_id
                        AND run.operator IS NULL
                  )
                  AND rr.reason LIKE 'MOOV:%'
              )
          )
    ),
    (
        SELECT CAST(ot.transaction_date_time AS DATE)
        FROM orange_transaction ot
        WHERE ot.id = rr.moov_transaction_id
          AND (
              EXISTS (
                  SELECT 1
                  FROM reconciliation_run run
                  WHERE run.id = rr.run_id
                    AND run.operator = 'ORANGE'
              )
              OR (
                  EXISTS (
                      SELECT 1
                      FROM reconciliation_run run
                      WHERE run.id = rr.run_id
                        AND run.operator IS NULL
                  )
                  AND rr.reason LIKE 'ORANGE:%'
              )
          )
    ),
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
          AND (
              EXISTS (
                  SELECT 1
                  FROM reconciliation_run run
                  WHERE run.id = rr.run_id
                    AND run.operator = 'MOOV'
              )
              OR (
                  EXISTS (
                      SELECT 1
                      FROM reconciliation_run run
                      WHERE run.id = rr.run_id
                        AND run.operator IS NULL
                  )
                  AND rr.reason LIKE 'MOOV:%'
              )
          )
    ),
    (
        SELECT fi.business_date
        FROM orange_transaction ot
        JOIN file_import fi ON fi.id = ot.import_id
        WHERE ot.id = rr.moov_transaction_id
          AND (
              EXISTS (
                  SELECT 1
                  FROM reconciliation_run run
                  WHERE run.id = rr.run_id
                    AND run.operator = 'ORANGE'
              )
              OR (
                  EXISTS (
                      SELECT 1
                      FROM reconciliation_run run
                      WHERE run.id = rr.run_id
                        AND run.operator IS NULL
                  )
                  AND rr.reason LIKE 'ORANGE:%'
              )
          )
    ),
    (
        SELECT run.business_date_from
        FROM reconciliation_run run
        WHERE run.id = rr.run_id
    )
) IS DISTINCT FROM rr.business_date;
