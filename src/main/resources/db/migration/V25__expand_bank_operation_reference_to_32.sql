ALTER TABLE bank_transaction
    ALTER COLUMN operation_reference TYPE VARCHAR(32);

-- Rebuild operation_reference from payload without 10-digit truncation.
UPDATE bank_transaction
SET operation_reference = NULLIF(
        regexp_replace(
                coalesce(
                        (regexp_match(raw_payload_json, '(?i)(?:^|[,{;\s])R\S*f\S*rence\s*op\S*ration\s*[:=]\s*"?([^,}\]";\s]+)"?'))[1],
                        (regexp_match(raw_payload_json, '(?i)(?:^|[,{;\s])Reference\s*operation\s*[:=]\s*"?([^,}\]";\s]+)"?'))[1]
                ),
                '[^0-9]',
                '',
                'g'
        ),
        ''
                        )
WHERE raw_payload_json IS NOT NULL;

