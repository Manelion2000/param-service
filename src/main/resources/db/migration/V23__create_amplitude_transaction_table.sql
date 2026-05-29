CREATE TABLE IF NOT EXISTS amplitude_transaction (
    id BIGSERIAL PRIMARY KEY,
    import_id BIGINT NOT NULL REFERENCES file_import(id) ON DELETE CASCADE,
    operation_reference VARCHAR(32),
    libelle VARCHAR(512),
    account_number VARCHAR(128),
    direction VARCHAR(64),
    amount NUMERIC(19,2),
    operation_date TIMESTAMP,
    raw_payload_json TEXT,
    line_number INTEGER
);

CREATE INDEX IF NOT EXISTS idx_amplitude_import_id ON amplitude_transaction(import_id);
CREATE INDEX IF NOT EXISTS idx_amplitude_operation_reference ON amplitude_transaction(operation_reference);
