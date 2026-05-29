ALTER TABLE moov_transaction
    ADD COLUMN IF NOT EXISTS transaction_type VARCHAR(32);

