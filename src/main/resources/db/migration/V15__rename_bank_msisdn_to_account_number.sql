ALTER TABLE bank_transaction
    ADD COLUMN IF NOT EXISTS account_number VARCHAR(64);

UPDATE bank_transaction
SET account_number = COALESCE(NULLIF(account_number, ''), NULLIF(msisdn, ''))
WHERE account_number IS NULL
   OR account_number = '';

ALTER TABLE bank_transaction
    DROP COLUMN IF EXISTS msisdn;
