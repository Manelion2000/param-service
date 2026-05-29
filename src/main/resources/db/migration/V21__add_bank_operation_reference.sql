ALTER TABLE bank_transaction
    ADD COLUMN IF NOT EXISTS operation_reference VARCHAR(10);

UPDATE bank_transaction
SET operation_reference = NULLIF(
        substring(
                regexp_replace(
                        coalesce(
                                (regexp_match(raw_payload_json, '(?i)(?:^|[,{;\\s])R\\S*f\\S*rence\\s*op\\S*ration\\s*[:=]\\s*"?([^,}\\]";\\s]+)"?'))[1],
                                (regexp_match(raw_payload_json, '(?i)(?:^|[,{;\\s])Reference\\s*operation\\s*[:=]\\s*"?([^,}\\]";\\s]+)"?'))[1]
                        ),
                        '[^0-9]',
                        '',
                        'g'
                ) from 1 for 10
        ),
        ''
                        )
WHERE (operation_reference IS NULL OR operation_reference = '')
  AND raw_payload_json IS NOT NULL;
