UPDATE bank_transaction
SET full_name = NULLIF(
        btrim(
                concat_ws(
                        ' ',
                        NULLIF(
                                btrim((regexp_match(raw_payload_json, '(?i)(?:^|[,{;\\s])Nom\\s*[:=]\\s*\"?([^,}\\]\";]+)\"?'))[1]),
                                ''
                        ),
                        NULLIF(
                                btrim((regexp_match(raw_payload_json, '(?i)(?:^|[,{;\\s])Pr\\S*nom\\s*[:=]\\s*\"?([^,}\\]\";]+)\"?'))[1]),
                                ''
                        )
                )
        ),
        ''
                )
WHERE raw_payload_json IS NOT NULL
  AND (full_name IS NULL OR full_name = '');
