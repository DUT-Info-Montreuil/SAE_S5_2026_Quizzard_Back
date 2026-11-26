-- Données de démonstration de l'UC7 (profil Spring « demo-uc7 ») : un enseignant, un cours et un quiz de 3 questions,
-- que l'application ne sait pas encore créer (UC1, UC2, UC4). Jamais dans une migration (DATABASE.md §2).
-- Rejouable : seules les lignes de démonstration (identifiants 910000…) sont supprimées puis recréées, avec les
-- séances lancées sur ce quiz.

DELETE FROM session_quiz WHERE id_quiz = 910000;
DELETE FROM page_inseree WHERE id_version = 910000;
DELETE FROM ecran_intermediaire WHERE id_question BETWEEN 910000 AND 910002;
DELETE FROM motif WHERE id_question BETWEEN 910000 AND 910002;
DELETE FROM paire WHERE id_question BETWEEN 910000 AND 910002;
DELETE FROM proposition WHERE id_question BETWEEN 910000 AND 910002;
DELETE FROM question WHERE id_quiz = 910000;
DELETE FROM quiz WHERE id_quiz = 910000;
DELETE FROM version_cours WHERE id_version = 910000;
DELETE FROM cours WHERE id_cours = 910000;
DELETE FROM enseignant WHERE id_enseignant = 910000;

INSERT INTO enseignant (id_enseignant, identifiant_ldap, nom, prenom, origine_compte, role, date_creation)
VALUES (910000, 'demo.lancement', 'Démo', 'Lancement', 'LOCAL', 'ENSEIGNANT', LOCALTIMESTAMP);

INSERT INTO cours (id_cours, titre, date_creation, date_modification, id_enseignant)
VALUES (910000, 'Réseaux (démo UC7)', LOCALTIMESTAMP, LOCALTIMESTAMP, 910000);

INSERT INTO version_cours (id_version, numero, nom_fichier, chemin_stockage, taille_octets, nb_pages, empreinte,
                           date_import, id_cours)
VALUES (910000, 1, 'demo-uc7.pdf', '/demo/demo-uc7.pdf', 1, 10, repeat('0', 64), LOCALTIMESTAMP, 910000);

INSERT INTO quiz (id_quiz, titre, est_note, duree_par_defaut_sec, affichage_cours, id_cours)
VALUES (910000, 'Quiz de démonstration UC7', FALSE, 30, 'PROJETE_SEUL', 910000);

INSERT INTO question (id_question, type, enonce, duree_sec, points_max, decroissance_temps, points_partiels,
                      id_quiz)
VALUES (910000, 'QCM', 'Quel protocole sert à l''annuaire de l''IUT ?', NULL, 1000, TRUE, FALSE, 910000),
       (910001, 'POINTS_A_RELIER', 'Relie chaque port à son protocole', 60, 1000, TRUE, TRUE, 910000),
       (910002, 'REPONSE_LIBRE', 'Quel protocole chiffre une page web ?', NULL, 1000, TRUE, FALSE, 910000);

INSERT INTO proposition (id_proposition, libelle, est_correcte, rang, id_question)
VALUES (910000, 'LDAP', TRUE, 0, 910000),
       (910001, 'SMTP', FALSE, 1, 910000),
       (910002, 'FTP', FALSE, 2, 910000),
       (910003, 'SSH', FALSE, 3, 910000);

INSERT INTO paire (id_paire, element_gauche, element_droit, id_question)
VALUES (910000, '80', 'HTTP', 910001),
       (910001, '53', 'DNS', 910001),
       (910002, '22', 'SSH', 910001);

INSERT INTO motif (motif, mode_motif, id_question)
VALUES ('https', 'SIMPLE', 910002),
       ('tls|ssl', 'REGEX', 910002);

INSERT INTO ecran_intermediaire (type_ecran, rang, delai_apres_cloture_sec, id_question)
VALUES ('BONNE_REPONSE', 0, 0, 910000),
       ('REPARTITION', 1, 5, 910000),
       ('CLASSEMENT', 2, 10, 910000);

INSERT INTO page_inseree (numero_page, ordre, valide, id_version, id_question)
VALUES (2, 0, TRUE, 910000, 910000),
       (4, 0, TRUE, 910000, 910001),
       (6, 0, TRUE, 910000, 910002);
