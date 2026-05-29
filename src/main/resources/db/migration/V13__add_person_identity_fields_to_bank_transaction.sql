ALTER TABLE bank_transaction
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS account_number VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_bank_transaction_account_number
    ON bank_transaction(account_number);
