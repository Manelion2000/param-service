ALTER TABLE reconciliation_run
    ADD COLUMN IF NOT EXISTS orange_import_ids TEXT;

CREATE TABLE IF NOT EXISTS orange_transaction (
    id BIGSERIAL PRIMARY KEY,
    import_id BIGINT NOT NULL REFERENCES file_import(id),
    alias_bank_account_number VARCHAR(255) NOT NULL,
    transaction_status_raw VARCHAR(255),
    transaction_status_normalized VARCHAR(24) NOT NULL,
    amount NUMERIC(19,2),
    transaction_date_time TIMESTAMP,
    raw_payload_json TEXT,
    line_number INTEGER
);

CREATE INDEX IF NOT EXISTS idx_orange_transaction_account_number
    ON orange_transaction(alias_bank_account_number);
CREATE INDEX IF NOT EXISTS idx_orange_transaction_import_id
    ON orange_transaction(import_id);
