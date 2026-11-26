package fr.iutmontreuil.quizzard.features.cours.domain;

/**
 * Page insérée d'une version, telle que listée pour l'éditeur (E3) : page de quiz ou page de classement.
 *
 * @param idQuestion question portée par une page de quiz ({@code null} pour une page de classement)
 * @param idQuiz     quiz de cette question ({@code null} pour une page de classement)
 * @param titreQuiz  titre de ce quiz ({@code null} pour une page de classement)
 */
public record PageInseree(Long idPage, int numeroPage, int ordre, TypePage type, Long idQuestion, Long idQuiz,
                          String titreQuiz) {

    public enum TypePage {
        QUIZ,
        CLASSEMENT
    }
}
