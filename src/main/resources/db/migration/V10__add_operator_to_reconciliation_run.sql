ALTER TABLE reconciliation_run
    ADD COLUMN IF NOT EXISTS operator VARCHAR(16);

CREATE INDEX IF NOT EXISTS idx_reconciliation_run_operator_started_at
    ON reconciliation_run(operator, started_at DESC);
