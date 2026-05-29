ALTER TABLE orange_transaction
    ADD COLUMN IF NOT EXISTS sender_mobile_number VARCHAR(64);

UPDATE orange_transaction
SET sender_mobile_number = NULLIF(
        regexp_replace(
                substring(raw_payload_json from '(?i)SENDER_MOBILE_NUMBER=([^,;\\s}\\]]+)'),
                '\\s+',
                '',
                'g'
        ),
        ''
                           )
WHERE sender_mobile_number IS NULL
  AND raw_payload_json IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_orange_transaction_sender_mobile_number
    ON orange_transaction(sender_mobile_number);
