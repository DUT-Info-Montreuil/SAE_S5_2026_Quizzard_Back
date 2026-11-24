package fr.iutmontreuil.quizzard.features.cours.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionIntrouvable;

/** Quiz inexistant, archivé ou d'un autre cours : 404. Code {@code QUIZ_INTROUVABLE} (UC3, tableau des cas d'erreur). */
public class QuizIntrouvableException extends ExceptionIntrouvable {

    public static final String CODE = "QUIZ_INTROUVABLE";

    public QuizIntrouvableException() {
        super("Quiz introuvable.");
    }

    @Override
    public String code() {
        return CODE;
    }
}
