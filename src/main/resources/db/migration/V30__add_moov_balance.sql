ALTER TABLE moov_transaction
    ADD COLUMN IF NOT EXISTS balance NUMERIC(19, 2);
