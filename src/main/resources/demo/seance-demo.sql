-- Données de démonstration (profil Spring « demo ») : remplacent « Lancer un quiz » (UC7) et « Rejoindre un quiz »
-- (UC8), pas encore codés, pour essayer l'UC9 avec le front. Jamais dans une migration (DATABASE.md §2).
-- Rejouable : seules les lignes de démonstration (identifiants 900000…) sont supprimées puis recréées.
-- Séance « DEMO », jeton du participant en clair : « jeton-demo » (seule son empreinte est stockée).

DELETE FROM session_quiz WHERE id_session = 900000;
DELETE FROM page_inseree WHERE id_version = 900000;
DELETE FROM motif WHERE id_question BETWEEN 900000 AND 900002;
DELETE FROM paire WHERE id_question BETWEEN 900000 AND 900002;
DELETE FROM proposition WHERE id_question BETWEEN 900000 AND 900002;
DELETE FROM question WHERE id_quiz = 900000;
DELETE FROM quiz WHERE id_quiz = 900000;
DELETE FROM version_cours WHERE id_version = 900000;
DELETE FROM cours WHERE id_cours = 900000;
DELETE FROM enseignant WHERE id_enseignant = 900000;

INSERT INTO enseignant (id_enseignant, identifiant_ldap, nom, prenom, origine_compte, role, date_creation)
VALUES (900000, 'demo.enseignant', 'Démo', 'Enseignant', 'LOCAL', 'ENSEIGNANT', LOCALTIMESTAMP);

INSERT INTO cours (id_cours, titre, date_creation, date_modification, id_enseignant)
VALUES (900000, 'Réseaux (démo)', LOCALTIMESTAMP, LOCALTIMESTAMP, 900000);

INSERT INTO version_cours (id_version, numero, nom_fichier, chemin_stockage, taille_octets, nb_pages, empreinte,
                           date_import, id_cours)
VALUES (900000, 1, 'demo.pdf', '/demo/demo.pdf', 1, 10, repeat('0', 64), LOCALTIMESTAMP, 900000);

INSERT INTO quiz (id_quiz, titre, est_note, duree_par_defaut_sec, affichage_cours, id_cours)
VALUES (900000, 'Quiz de démonstration', FALSE, 60, 'PROJETE_SEUL', 900000);

INSERT INTO question (id_question, type, enonce, duree_sec, points_max, decroissance_temps, points_partiels,
                      id_quiz)
VALUES (900000, 'QCM', 'Quel protocole sert à l''annuaire de l''IUT ?', NULL, 1000, TRUE, FALSE, 900000),
       (900001, 'POINTS_A_RELIER', 'Relie chaque port à son protocole', 90, 1000, TRUE, TRUE, 900000),
       (900002, 'REPONSE_LIBRE', 'Quel protocole chiffre une page web ?', NULL, 1000, TRUE, FALSE, 900000);

INSERT INTO proposition (id_proposition, libelle, est_correcte, rang, id_question)
VALUES (900000, 'LDAP', TRUE, 0, 900000),
       (900001, 'SMTP', FALSE, 1, 900000),
       (900002, 'FTP', FALSE, 2, 900000),
       (900003, 'SSH', FALSE, 3, 900000);

INSERT INTO paire (id_paire, element_gauche, element_droit, id_question)
VALUES (900000, '80', 'HTTP', 900001),
       (900001, '53', 'DNS', 900001),
       (900002, '22', 'SSH', 900001);

INSERT INTO motif (motif, mode_motif, id_question)
VALUES ('https', 'SIMPLE', 900002),
       ('tls|ssl', 'REGEX', 900002);

INSERT INTO page_inseree (numero_page, ordre, valide, id_version, id_question)
VALUES (2, 0, TRUE, 900000, 900000),
       (4, 0, TRUE, 900000, 900001),
       (6, 0, TRUE, 900000, 900002);

INSERT INTO session_quiz (id_session, code_acces, etat, est_notee, date_lancement, position_courante, id_quiz,
                          id_version)
VALUES (900000, 'DEMO', 'PREPAREE', FALSE, LOCALTIMESTAMP, 0, 900000, 900000);

INSERT INTO participant (id_participant, pseudonyme, identite_verifiee, score_total, date_entree,
                         jeton_reconnexion, derniere_activite, id_session)
VALUES (900000, 'Demo', TRUE, 0, LOCALTIMESTAMP, encode(sha256(convert_to('jeton-demo', 'UTF8')), 'hex'),
        LOCALTIMESTAMP, 900000);
