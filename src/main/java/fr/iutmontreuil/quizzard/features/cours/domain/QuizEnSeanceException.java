package fr.iutmontreuil.quizzard.features.cours.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflit;

/** Une séance du quiz est en cours : 409. Code {@code QUIZ_EN_SEANCE} (UC3, tableau des cas d'erreur). */
public class QuizEnSeanceException extends ExceptionConflit {

    public static final String CODE = "QUIZ_EN_SEANCE";

    public QuizEnSeanceException() {
        super("Ce quiz est en cours de séance : réessayez après sa clôture.");
    }

    @Override
    public String code() {
        return CODE;
    }
}
