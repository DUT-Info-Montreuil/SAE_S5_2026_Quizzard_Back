package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflit;

/** Il n'y a pas de question à clore : 409 {@code AUCUNE_QUESTION_OUVERTE}. */
public class AucuneQuestionOuverteException extends ExceptionConflit {

    public AucuneQuestionOuverteException() {
        super("Aucune question n'est ouverte.");
    }

    @Override
    public String code() {
        return "AUCUNE_QUESTION_OUVERTE";
    }
}
