CREATE TABLE IF NOT EXISTS reconciliation_run_archive (
  id BIGSERIAL PRIMARY KEY,
  source_run_id BIGINT NOT NULL UNIQUE,
  label VARCHAR(255) NOT NULL,
  business_date_from DATE,
  business_date_to DATE,
  bank_import_ids TEXT,
  moov_import_ids TEXT,
  started_at TIMESTAMPTZ NOT NULL,
  finished_at TIMESTAMPTZ,
  status VARCHAR(16) NOT NULL,
  summary_json TEXT,
  archived_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS reconciliation_result_archive (
  id BIGSERIAL PRIMARY KEY,
  source_result_id BIGINT NOT NULL UNIQUE,
  run_id BIGINT NOT NULL,
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
  created_at TIMESTAMPTZ NOT NULL,
  archived_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reconciliation_run_archive_source_run_id
  ON reconciliation_run_archive(source_run_id);

CREATE INDEX IF NOT EXISTS idx_reconciliation_run_archive_business_date_to
  ON reconciliation_run_archive(business_date_to);

CREATE INDEX IF NOT EXISTS idx_reconciliation_result_archive_source_result_id
  ON reconciliation_result_archive(source_result_id);

CREATE INDEX IF NOT EXISTS idx_reconciliation_result_archive_run_id
  ON reconciliation_result_archive(run_id);

CREATE INDEX IF NOT EXISTS idx_reconciliation_result_archive_business_date
  ON reconciliation_result_archive(business_date);
