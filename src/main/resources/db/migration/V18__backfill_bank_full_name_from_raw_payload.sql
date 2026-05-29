UPDATE bank_transaction
SET full_name = NULLIF(
        btrim(
                concat_ws(
                        ' ',
                        NULLIF(btrim(COALESCE(
                                substring(raw_payload_json from '(?i)Nom\\s*=\\s*([^,}\\]]+)'),
                                substring(raw_payload_json from '(?i)NOM\\s*=\\s*([^,}\\]]+)')
                        )), ''),
                        NULLIF(btrim(COALESCE(
                                substring(raw_payload_json from '(?i)Prénom\\s*=\\s*([^,}\\]]+)'),
                                substring(raw_payload_json from '(?i)Prenom\\s*=\\s*([^,}\\]]+)'),
                                substring(raw_payload_json from '(?i)PrÃ©nom\\s*=\\s*([^,}\\]]+)')
                        )), '')
                )
        ),
        ''
                )
WHERE raw_payload_json IS NOT NULL
  AND (full_name IS NULL OR full_name = '')
  AND (
        substring(raw_payload_json from '(?i)Nom\\s*=\\s*([^,}\\]]+)') IS NOT NULL
        OR substring(raw_payload_json from '(?i)NOM\\s*=\\s*([^,}\\]]+)') IS NOT NULL
        OR substring(raw_payload_json from '(?i)Prénom\\s*=\\s*([^,}\\]]+)') IS NOT NULL
        OR substring(raw_payload_json from '(?i)Prenom\\s*=\\s*([^,}\\]]+)') IS NOT NULL
        OR substring(raw_payload_json from '(?i)PrÃ©nom\\s*=\\s*([^,}\\]]+)') IS NOT NULL
      );
