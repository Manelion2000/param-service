ALTER TABLE file_import
    ADD COLUMN IF NOT EXISTS operator_scope VARCHAR(16);

CREATE INDEX IF NOT EXISTS idx_file_import_source_operator_business_date
    ON file_import(source_type, operator_scope, business_date);
