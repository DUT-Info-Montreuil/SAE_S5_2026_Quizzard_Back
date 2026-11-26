package fr.iutmontreuil.quizzard.features.seance.application.dto;

/**
 * Données reçues pour lancer un quiz (UC7).
 *
 * @param idEnseignant                 enseignant de la session de connexion, jamais lu dans la requête
 * @param autoriserIdentiteNonVerifiee utile seulement pour un quiz noté quand l'annuaire est indisponible
 */
public record LancerQuizDto(Long idQuiz, Long idEnseignant, boolean autoriserIdentiteNonVerifiee) {
}
