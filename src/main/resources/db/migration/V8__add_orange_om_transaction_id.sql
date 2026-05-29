ALTER TABLE orange_transaction
    ADD COLUMN IF NOT EXISTS om_transaction_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_orange_transaction_om_transaction_id
    ON orange_transaction(om_transaction_id);
