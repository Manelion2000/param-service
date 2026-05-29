CREATE UNIQUE INDEX IF NOT EXISTS uq_file_import_scope_checksum
    ON file_import(source_type, business_date, COALESCE(operator_scope, '__NONE__'), checksum);
