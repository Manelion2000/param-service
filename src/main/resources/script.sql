-- Table ba_role
INSERT INTO ba_role (created_by, created_date, last_modified_by, last_modified_date, id, code, libelle)
VALUES ('yabolank', '2024-05-24 14:48:47.300', 'yabolank', '2024-05-24 14:48:47.309',
        '4ab',
        'BA_ADMIN', 'Administrateur'),
       ('yabolank', '2024-05-24 14:48:47.300', 'yabolank', '2024-05-24 14:48:47.309',
        'e28',
        'BA_CONNECT', 'Connexion');

-- Table ba_profil
INSERT INTO ba_profil (statut, created_by, created_date, last_modified_by, last_modified_date, id, libelle)
VALUES ('A', 'yabolank', '2024-05-24 14:48:47.300', 'yabolank', '2024-05-24 14:48:47.309',
        '1e', 'Administrateur'),
       ('A', 'yabolank', '2024-05-24 14:48:47.300', 'yabolank', '2024-05-24 14:48:47.309',
        'ce9', 'Membre');

-- Table ba_profils_roles(admin)
INSERT INTO ba_profils_roles (profil_id, role_id)
VALUES ('1e', '4ab'),
       ('1e', 'e28');

-- Table ba_profils_roles(membre)
INSERT INTO ba_profils_roles (profil_id, role_id)
VALUES ('ce9', 'e28');


-- Utilisateurs
INSERT INTO ba_utilisateur (id, created_by, created_date, email, account_locked, nom, statut, password_hash, prenom,
                            telephone, username, profil_uuid, activated, sexe)
VALUES ('51a0379b-2f2f-4eaa-aad3-e41d88dc0db1', 'abakouan', '2021-02-05 15:16:06.0', 'abdramanbakouan@gmail.com',
        false, 'BAKOUAN', 'A',
        '$2a$10$5EykYe4F6osi7rrZEghdVueK.McdyIGjHnbjYJIUiIHkVb7EGMPES', 'ABDRAMAN', '+22672808708', 'abakouan',
        '1e', true, 'MASCULIN');
