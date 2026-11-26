package fr.iutmontreuil.quizzard.features.cours.domain;

/**
 * Page insérée vue pour une modification (retrait, déplacement) : sa place et ce qu'il faut contrôler avant de la
 * toucher (cours propriétaire, version, quiz éventuellement en séance).
 *
 * @param idQuiz quiz de la question portée par la page, ou {@code null} pour une page de classement
 */
public record PagePlacee(Long idPage, Long idVersion, Long idCours, int numeroPage, int ordre, Long idQuiz) {
}
