ALTER TABLE ba_utilisateur
    ADD COLUMN IF NOT EXISTS password_reset_required BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO ba_role (id, libelle, code, created_by, created_date, statut, version)
VALUES
    ('role-ba-admin', 'ADMIN', 'BA_ADMIN', 'system', NOW(), 'A', 1),
    ('role-ba-agent', 'AGENT', 'BA_AGENT', 'system', NOW(), 'A', 1),
    ('role-ba-connect', 'UTILISATEUR CONNECTE', 'BA_CONNECT', 'system', NOW(), 'A', 1)
ON CONFLICT (code) DO NOTHING;

INSERT INTO ba_profil (id, libelle, description, created_by, created_date, statut, version)
VALUES
    ('profil-admin', 'ADMIN', 'Administrateur de la plateforme', 'system', NOW(), 'A', 1),
    ('profil-agent', 'AGENT', 'Agent de rapprochement', 'system', NOW(), 'A', 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO ba_profils_roles (profil_id, role_id)
SELECT 'profil-admin', id FROM ba_role WHERE code = 'BA_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO ba_profils_roles (profil_id, role_id)
SELECT 'profil-admin', id FROM ba_role WHERE code = 'BA_AGENT'
ON CONFLICT DO NOTHING;

INSERT INTO ba_profils_roles (profil_id, role_id)
SELECT 'profil-admin', id FROM ba_role WHERE code = 'BA_CONNECT'
ON CONFLICT DO NOTHING;

INSERT INTO ba_profils_roles (profil_id, role_id)
SELECT 'profil-agent', id FROM ba_role WHERE code = 'BA_AGENT'
ON CONFLICT DO NOTHING;

INSERT INTO ba_profils_roles (profil_id, role_id)
SELECT 'profil-agent', id FROM ba_role WHERE code = 'BA_CONNECT'
ON CONFLICT DO NOTHING;
