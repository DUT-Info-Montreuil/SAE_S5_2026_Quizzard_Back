package fr.iutmontreuil.quizzard.features.cours.application.dto;

/**
 * Demande d'insertion d'une page de quiz. L'enseignant n'y figure pas : il est lu dans la session de connexion.
 *
 * @param idQuiz      quiz qui reçoit la question, ou {@code null} pour créer un nouveau quiz
 * @param repereTexte texte de la page qui précède (extrait par le navigateur), facultatif
 */
public record InsererPageQuizDto(Long idCours, Long idVersion, int numeroPage, int ordre, Long idQuiz,
                                 String repereTexte) {
}
