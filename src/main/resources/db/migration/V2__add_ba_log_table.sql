CREATE TABLE IF NOT EXISTS ba_log (
  id VARCHAR(255) PRIMARY KEY,
  action INTEGER,
  ip_adresse VARCHAR(255),
  sujet VARCHAR(255),
  details VARCHAR(255),
  created_by VARCHAR(50) NOT NULL,
  created_date TIMESTAMPTZ NOT NULL,
  last_modified_by VARCHAR(50),
  last_modified_date TIMESTAMPTZ,
  statut VARCHAR(255),
  version BIGINT
);
