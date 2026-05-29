ALTER TABLE bank_transaction
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(32);

UPDATE bank_transaction
SET phone_number = NULLIF(
        regexp_replace(
                coalesce(
                        (regexp_match(raw_payload_json, '(?i)(?:^|[,{;\\s])MSISDN\\s*[:=]\\s*"?([^,}\\]";\\s]+)"?'))[1],
                        (regexp_match(raw_payload_json, '(?i)(?:^|[,{;\\s])Telephone\\s*[:=]\\s*"?([^,}\\]";\\s]+)"?'))[1],
                        (regexp_match(raw_payload_json, '(?i)(?:^|[,{;\\s])T\\S*l\\S*phone\\s*[:=]\\s*"?([^,}\\]";\\s]+)"?'))[1]
                ),
                '[^0-9]',
                '',
                'g'
        ),
        ''
                   )
WHERE (phone_number IS NULL OR phone_number = '')
  AND raw_payload_json IS NOT NULL;
