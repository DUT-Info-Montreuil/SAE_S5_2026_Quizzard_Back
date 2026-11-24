package fr.iutmontreuil.quizzard.features.cours.domain;

/** Page insérée d'une version, telle que listée pour l'éditeur (E3) : page de quiz ou page de classement. */
public record PageInseree(Long idPage, int numeroPage, int ordre, TypePage type) {

    public enum TypePage {
        QUIZ,
        CLASSEMENT
    }
}
