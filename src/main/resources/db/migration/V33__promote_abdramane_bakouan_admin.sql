UPDATE ba_utilisateur
SET profil_uuid = 'profil-admin',
    activated = TRUE,
    account_locked = FALSE,
    last_modified_by = 'system',
    last_modified_date = NOW()
WHERE LOWER(username) = 'abdramane.bakouan@bsic.bf'
   OR LOWER(email) = 'abdramane.bakouan@bsic.bf';
