UPDATE bank_transaction
SET account_number = NULLIF(
        regexp_replace(
                COALESCE(
                        substring(raw_payload_json from '(?i)Numéro de compte\\s*=\\s*([^,}\\]]+)'),
                        substring(raw_payload_json from '(?i)Numero de compte\\s*=\\s*([^,}\\]]+)'),
                        substring(raw_payload_json from '(?i)NumÃ©ro de compte\\s*=\\s*([^,}\\]]+)')
                ),
                '[^0-9]',
                '',
                'g'
        ),
        ''
                     )
WHERE raw_payload_json IS NOT NULL
  AND COALESCE(
        substring(raw_payload_json from '(?i)Numéro de compte\\s*=\\s*([^,}\\]]+)'),
        substring(raw_payload_json from '(?i)Numero de compte\\s*=\\s*([^,}\\]]+)'),
        substring(raw_payload_json from '(?i)NumÃ©ro de compte\\s*=\\s*([^,}\\]]+)')
      ) IS NOT NULL;
