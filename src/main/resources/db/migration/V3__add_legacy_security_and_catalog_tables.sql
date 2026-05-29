CREATE TABLE IF NOT EXISTS ba_role (
  id VARCHAR(255) PRIMARY KEY,
  libelle VARCHAR(255) NOT NULL UNIQUE,
  code VARCHAR(255) NOT NULL UNIQUE,
  created_by VARCHAR(50) NOT NULL,
  created_date TIMESTAMPTZ NOT NULL,
  last_modified_by VARCHAR(50),
  last_modified_date TIMESTAMPTZ,
  statut VARCHAR(255),
  version BIGINT
);

CREATE TABLE IF NOT EXISTS ba_profil (
  id VARCHAR(255) PRIMARY KEY,
  libelle VARCHAR(60) NOT NULL,
  description VARCHAR(255),
  created_by VARCHAR(50) NOT NULL,
  created_date TIMESTAMPTZ NOT NULL,
  last_modified_by VARCHAR(50),
  last_modified_date TIMESTAMPTZ,
  statut VARCHAR(255),
  version BIGINT
);

CREATE TABLE IF NOT EXISTS ba_profils_roles (
  profil_id VARCHAR(255) NOT NULL,
  role_id VARCHAR(255) NOT NULL,
  PRIMARY KEY (profil_id, role_id),
  CONSTRAINT fk_ba_profils_roles_profil FOREIGN KEY (profil_id) REFERENCES ba_profil(id),
  CONSTRAINT fk_ba_profils_roles_role FOREIGN KEY (role_id) REFERENCES ba_role(id)
);

CREATE TABLE IF NOT EXISTS ba_utilisateur (
  id VARCHAR(255) PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(254) NOT NULL,
  nom VARCHAR(50),
  prenom VARCHAR(50),
  email VARCHAR(100),
  account_locked BOOLEAN NOT NULL,
  reset_key VARCHAR(20),
  reset_date TIMESTAMPTZ,
  last_connexion_date TIMESTAMPTZ,
  telephone VARCHAR(255) NOT NULL UNIQUE,
  profil_uuid VARCHAR(255),
  activated BOOLEAN,
  sexe VARCHAR(255),
  indicatif_pays VARCHAR(255),
  created_by VARCHAR(50) NOT NULL,
  created_date TIMESTAMPTZ NOT NULL,
  last_modified_by VARCHAR(50),
  last_modified_date TIMESTAMPTZ,
  statut VARCHAR(255),
  version BIGINT,
  CONSTRAINT fk_ba_utilisateur_profil FOREIGN KEY (profil_uuid) REFERENCES ba_profil(id)
);

CREATE TABLE IF NOT EXISTS ba_categorie (
  id VARCHAR(255) PRIMARY KEY,
  code VARCHAR(255),
  nom VARCHAR(255),
  created_by VARCHAR(50) NOT NULL,
  created_date TIMESTAMPTZ NOT NULL,
  last_modified_by VARCHAR(50),
  last_modified_date TIMESTAMPTZ,
  statut VARCHAR(255),
  version BIGINT
);

CREATE TABLE IF NOT EXISTS ba_product (
  id VARCHAR(255) PRIMARY KEY,
  name VARCHAR(255),
  description VARCHAR(255),
  price DOUBLE PRECISION,
  categorie VARCHAR(255),
  CONSTRAINT fk_ba_product_categorie FOREIGN KEY (categorie) REFERENCES ba_categorie(id)
);
