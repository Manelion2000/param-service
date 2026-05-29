CREATE TABLE IF NOT EXISTS file_import (
  id BIGSERIAL PRIMARY KEY,
  source_type VARCHAR(16) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  stored_filename VARCHAR(255) NOT NULL,
  content_type VARCHAR(255),
  checksum VARCHAR(128),
  business_date DATE NOT NULL,
  imported_at TIMESTAMPTZ NOT NULL,
  total_rows INTEGER,
  valid_rows INTEGER,
  invalid_rows INTEGER,
  import_status VARCHAR(24) NOT NULL,
  error_message TEXT,
  file_path VARCHAR(512) NOT NULL
);

CREATE TABLE IF NOT EXISTS bank_transaction (
  id BIGSERIAL PRIMARY KEY,
  import_id BIGINT NOT NULL REFERENCES file_import(id),
  transaction_id VARCHAR(255) NOT NULL,
  allocation_status_raw VARCHAR(255),
  allocation_status_normalized VARCHAR(24) NOT NULL,
  msisdn VARCHAR(64),
  amount NUMERIC(19,2),
  transaction_date TIMESTAMP,
  raw_payload_json TEXT,
  line_number INTEGER
);

CREATE TABLE IF NOT EXISTS moov_transaction (
  id BIGSERIAL PRIMARY KEY,
  import_id BIGINT NOT NULL REFERENCES file_import(id),
  receipt_no VARCHAR(255) NOT NULL,
  transaction_status_raw VARCHAR(255),
  transaction_status_normalized VARCHAR(24) NOT NULL,
  msisdn VARCHAR(64),
  amount NUMERIC(19,2),
  initiation_time TIMESTAMP,
  completion_time TIMESTAMP,
  raw_payload_json TEXT,
  line_number INTEGER
);

CREATE TABLE IF NOT EXISTS reconciliation_run (
  id BIGSERIAL PRIMARY KEY,
  label VARCHAR(255) NOT NULL,
  business_date_from DATE,
  business_date_to DATE,
  bank_import_ids TEXT,
  moov_import_ids TEXT,
  started_at TIMESTAMPTZ NOT NULL,
  finished_at TIMESTAMPTZ,
  status VARCHAR(16) NOT NULL,
  summary_json TEXT
);

CREATE TABLE IF NOT EXISTS reconciliation_result (
  id BIGSERIAL PRIMARY KEY,
  run_id BIGINT NOT NULL REFERENCES reconciliation_run(id),
  business_date DATE,
  transaction_key VARCHAR(255) NOT NULL,
  result_type VARCHAR(32) NOT NULL,
  bank_transaction_id BIGINT,
  moov_transaction_id BIGINT,
  bank_status_raw VARCHAR(255),
  moov_status_raw VARCHAR(255),
  bank_amount NUMERIC(19,2),
  moov_amount NUMERIC(19,2),
  amount_difference NUMERIC(19,2),
  reason TEXT,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_bank_transaction_transaction_id ON bank_transaction(transaction_id);
CREATE INDEX IF NOT EXISTS idx_bank_transaction_import_id ON bank_transaction(import_id);
CREATE INDEX IF NOT EXISTS idx_moov_transaction_receipt_no ON moov_transaction(receipt_no);
CREATE INDEX IF NOT EXISTS idx_moov_transaction_import_id ON moov_transaction(import_id);
CREATE INDEX IF NOT EXISTS idx_file_import_business_date ON file_import(business_date);
CREATE INDEX IF NOT EXISTS idx_reconciliation_result_result_type ON reconciliation_result(result_type);
CREATE INDEX IF NOT EXISTS idx_reconciliation_result_run_id ON reconciliation_result(run_id);
