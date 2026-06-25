ALTER TABLE bank_transaction
    ADD COLUMN IF NOT EXISTS reject_reason_raw VARCHAR(512);

