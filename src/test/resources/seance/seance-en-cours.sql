-- Jeu de test de l'UC9 : une séance en cours dont la première question est ouverte.
-- Remplace les cas « Lancer un quiz » (UC7) et « Rejoindre un quiz » (UC8), pas encore codés.
-- Quiz de 3 questions, dans l'ordre des pages insérées : QCM (1000), points à relier (1001), réponse libre (1002).
-- L'échéance de la question ouverte est fixée par le test, avec l'horloge du serveur.
TRUNCATE enseignant CASCADE;

INSERT INTO enseignant (id_enseignant, identifiant_ldap, nom, prenom, origine_compte, role, mot_de_passe_hash,
                        date_creation)
VALUES (1000, 'enseignant.test', 'Test', 'Enseignant', 'LOCAL', 'ENSEIGNANT', NULL, LOCALTIMESTAMP);

INSERT INTO cours (id_cours, titre, date_creation, date_modification, id_enseignant)
VALUES (1000, 'Réseaux', LOCALTIMESTAMP, LOCALTIMESTAMP, 1000);

INSERT INTO version_cours (id_version, numero, nom_fichier, chemin_stockage, taille_octets, nb_pages, empreinte,
                           date_import, id_cours)
VALUES (1000, 1, 'reseaux.pdf', '/stockage/reseaux.pdf', 1024, 10, repeat('a', 64), LOCALTIMESTAMP, 1000);

INSERT INTO quiz (id_quiz, titre, est_note, duree_par_defaut_sec, affichage_cours, id_cours)
VALUES (1000, 'Quiz réseaux', FALSE, 20, 'PROJETE_SEUL', 1000);

-- Questions (durée NULL = 20 s, celle du quiz) ; 1000 points avec décroissance, sans points partiels sauf relier.
INSERT INTO question (id_question, type, enonce, duree_sec, points_max, decroissance_temps, points_partiels,
                      id_quiz)
VALUES (1000, 'QCM', 'Quel protocole sert à l''annuaire de l''IUT ?', NULL, 1000, TRUE, FALSE, 1000),
       (1001, 'POINTS_A_RELIER', 'Relie chaque port à son protocole', 30, 1000, TRUE, TRUE, 1000),
       (1002, 'REPONSE_LIBRE', 'Quel protocole sert à l''annuaire de l''IUT ?', NULL, 1000, TRUE, FALSE, 1000);

INSERT INTO proposition (id_proposition, libelle, est_correcte, rang, id_question)
VALUES (1000, 'LDAP', TRUE, 0, 1000),
       (1001, 'SMTP', FALSE, 1, 1000),
       (1002, 'FTP', FALSE, 2, 1000);

INSERT INTO paire (id_paire, element_gauche, element_droit, id_question)
VALUES (1000, '80', 'HTTP', 1001),
       (1001, '53', 'DNS', 1001),
       (1002, '22', 'SSH', 1001);

INSERT INTO motif (motif, mode_motif, id_question)
VALUES ('ldap', 'SIMPLE', 1002),
       ('(protocole )?ldaps?', 'REGEX', 1002);

-- Pages de quiz : la question 1000 après la page 2, puis 1001 et 1002 après la page 5.
INSERT INTO page_inseree (numero_page, ordre, valide, id_version, id_question)
VALUES (5, 1, TRUE, 1000, 1002),
       (2, 0, TRUE, 1000, 1000),
       (5, 0, TRUE, 1000, 1001);

INSERT INTO session_quiz (id_session, code_acces, etat, est_notee, date_lancement, position_courante,
                          echeance_question, id_quiz, id_version)
VALUES (1000, 'QUIZ-UC9', 'EN_COURS', FALSE, LOCALTIMESTAMP, 1, NULL, 1000, 1000);

-- Jeton en clair : « jeton-de-test-uc9 » ; seule son empreinte SHA-256 est stockée.
INSERT INTO participant (id_participant, pseudonyme, identite_verifiee, score_total, date_entree,
                         jeton_reconnexion, derniere_activite, id_session)
VALUES (1000, 'Alice', TRUE, 0, LOCALTIMESTAMP,
        encode(sha256(convert_to('jeton-de-test-uc9', 'UTF8')), 'hex'), LOCALTIMESTAMP, 1000);
