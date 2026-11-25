package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflit;

/** La séance n'est pas en cours ou la question visée n'est pas la question ouverte : 409. Code {@code QUESTION_CLOSE} (UC9, tableau des cas d'erreur). */
public class QuestionCloseException extends ExceptionConflit {

    public static final String CODE = "QUESTION_CLOSE";

    public QuestionCloseException() {
        super("Cette session est terminée.");
    }

    @Override
    public String code() {
        return CODE;
    }
}
