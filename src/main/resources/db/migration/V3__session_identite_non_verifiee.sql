-- =====================================================================================
-- V3 : autorisation d'entrée en « identité non vérifiée » (UC7 « Lancer un quiz en séance »)
-- La V2 est prise par l'UC3 (branche feature/uc3-inserer-page-quiz).
--
-- Au lancement d'un quiz noté, si l'annuaire LDAP ne répond pas, l'enseignant peut autoriser les étudiants à entrer
-- en identité non vérifiée (dossier §3.1.8.4). La décision appartient à la séance : « S'authentifier à un quiz noté »
-- (UC15) la lira.
-- =====================================================================================

ALTER TABLE session_quiz ADD COLUMN identite_non_verifiee_autorisee BOOLEAN NOT NULL DEFAULT FALSE;
