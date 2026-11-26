-- Complète cours-a-modifier.sql pour lister, retirer et déplacer des pages (éditeur E3).
-- Version 2001 (dernière, 12 pages) du cours 2000 : (2,0) page 2000, (2,1) page 2001, (5,0) page 2002 du quiz 2000,
-- et (7,0) page 2003 du quiz 2001, en cours de séance.
-- Pages qu'on ne peut pas modifier : 2004 (version 2000 périmée), 2005 (cours d'un autre enseignant),
-- 2006 (cours archivé). Quiz du cours 2000 sans page : 2004 (archivé) et 2005.
INSERT INTO quiz (id_quiz, titre, est_note, duree_par_defaut_sec, affichage_cours, archive, id_cours)
VALUES (2003, 'Quiz du cours archivé', FALSE, 20, 'PROJETE_SEUL', FALSE, 2001),
       (2004, 'Quiz archivé', FALSE, 20, 'PROJETE_SEUL', TRUE, 2000),
       (2005, 'Quiz sans page', FALSE, 20, 'PROJETE_SEUL', FALSE, 2000);

INSERT INTO question (id_question, type, enonce, points_max, decroissance_temps, points_partiels, id_quiz)
VALUES (2001, 'QCM', 'Q2', 1000, TRUE, FALSE, 2000),
       (2002, 'QCM', 'Q3', 1000, TRUE, FALSE, 2000),
       (2003, 'QCM', 'En séance', 1000, TRUE, FALSE, 2001),
       (2004, 'QCM', 'Ancienne version', 1000, TRUE, FALSE, 2000),
       (2005, 'QCM', 'Autre enseignant', 1000, TRUE, FALSE, 2002),
       (2006, 'QCM', 'Cours archivé', 1000, TRUE, FALSE, 2003);

INSERT INTO page_inseree (id_page, numero_page, ordre, valide, id_version, id_question)
VALUES (2001, 2, 1, TRUE, 2001, 2001),
       (2002, 5, 0, TRUE, 2001, 2002),
       (2003, 7, 0, TRUE, 2001, 2003),
       (2004, 1, 0, TRUE, 2000, 2004),
       (2005, 1, 0, TRUE, 2003, 2005),
       (2006, 1, 0, TRUE, 2002, 2006);
