-- Jeu de test de l'UC3 : un cours de 12 pages (2 versions) de l'enseignant « enseignant.test », un quiz avec une
-- page déjà insérée en (2, 0), un quiz en cours de séance, et le cours d'un autre enseignant.
-- Remplace l'import d'un PDF (UC2) et la connexion (UC1), pas encore codés.
TRUNCATE enseignant CASCADE;

INSERT INTO enseignant (id_enseignant, identifiant_ldap, nom, prenom, origine_compte, date_creation)
VALUES (2000, 'enseignant.test', 'Test', 'Enseignant', 'LOCAL', LOCALTIMESTAMP),
       (2001, 'autre.enseignant', 'Autre', 'Enseignant', 'LOCAL', LOCALTIMESTAMP);

INSERT INTO cours (id_cours, titre, date_creation, date_modification, archive, id_enseignant)
VALUES (2000, 'Réseaux', LOCALTIMESTAMP, '2026-01-01 00:00:00', FALSE, 2000),
       (2001, 'Cours archivé', LOCALTIMESTAMP, LOCALTIMESTAMP, TRUE, 2000),
       (2002, 'Cours d''un autre', LOCALTIMESTAMP, LOCALTIMESTAMP, FALSE, 2001);

-- Cours 2000 : version 1 (périmée) puis version 2 (dernière, 12 pages).
INSERT INTO version_cours (id_version, numero, nom_fichier, chemin_stockage, taille_octets, nb_pages, empreinte,
                           date_import, id_cours)
VALUES (2000, 1, 'v1.pdf', '/s/v1.pdf', 1, 10, repeat('a', 64), LOCALTIMESTAMP, 2000),
       (2001, 2, 'v2.pdf', '/s/v2.pdf', 1, 12, repeat('b', 64), LOCALTIMESTAMP, 2000),
       (2002, 1, 'a.pdf', '/s/a.pdf', 1, 5, repeat('c', 64), LOCALTIMESTAMP, 2001),
       (2003, 1, 'o.pdf', '/s/o.pdf', 1, 5, repeat('d', 64), LOCALTIMESTAMP, 2002);

INSERT INTO quiz (id_quiz, titre, est_note, duree_par_defaut_sec, affichage_cours, archive, id_cours)
VALUES (2000, 'Quiz 1', FALSE, 20, 'PROJETE_SEUL', FALSE, 2000),
       (2001, 'Quiz en séance', FALSE, 20, 'PROJETE_SEUL', FALSE, 2000),
       (2002, 'Quiz de l''autre', FALSE, 20, 'PROJETE_SEUL', FALSE, 2002);

INSERT INTO question (id_question, type, enonce, points_max, decroissance_temps, points_partiels, id_quiz)
VALUES (2000, 'QCM', 'Déjà là', 1000, TRUE, FALSE, 2000);

INSERT INTO page_inseree (id_page, numero_page, ordre, valide, id_version, id_question)
VALUES (2000, 2, 0, TRUE, 2001, 2000);

INSERT INTO session_quiz (id_session, code_acces, etat, est_notee, position_courante, id_quiz, id_version)
VALUES (2000, 'EN-SEANCE', 'EN_COURS', FALSE, 1, 2001, 2001);
