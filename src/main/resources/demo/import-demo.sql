-- Données de démonstration de l'UC2 (profil Spring « demo-uc2 ») : un enseignant, que l'application ne sait pas
-- encore connecter (UC1). Jamais dans une migration (DATABASE.md §2).
-- Rejouable : l'enseignant n'est créé qu'une fois, les cours qu'il a importés sont gardés.

INSERT INTO enseignant (id_enseignant, identifiant_ldap, nom, prenom, origine_compte, role, date_creation)
VALUES (920000, 'demo.import', 'Démo', 'Import', 'LOCAL', 'ENSEIGNANT', LOCALTIMESTAMP)
ON CONFLICT (id_enseignant) DO NOTHING;
