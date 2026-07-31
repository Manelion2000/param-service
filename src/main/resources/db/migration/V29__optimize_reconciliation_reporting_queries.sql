CREATE INDEX IF NOT EXISTS idx_reconciliation_run_operator_started_at
    ON reconciliation_run(operator, started_at);

CREATE INDEX IF NOT EXISTS idx_reconciliation_run_operator_business_period
    ON reconciliation_run(operator, business_date_from, business_date_to);

CREATE INDEX IF NOT EXISTS idx_reconciliation_result_run_business_date
    ON reconciliation_result(run_id, business_date);

CREATE INDEX IF NOT EXISTS idx_bank_transaction_transaction_date
    ON bank_transaction(transaction_date);

CREATE INDEX IF NOT EXISTS idx_moov_transaction_completion_time
    ON moov_transaction(completion_time);

CREATE INDEX IF NOT EXISTS idx_orange_transaction_transaction_datetime
    ON orange_transaction(transaction_date_time);
