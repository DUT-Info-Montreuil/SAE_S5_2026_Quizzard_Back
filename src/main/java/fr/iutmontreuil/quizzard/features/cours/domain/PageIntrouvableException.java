package fr.iutmontreuil.quizzard.features.cours.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionIntrouvable;

/** Page inexistante ou d'un cours d'un autre enseignant (même réponse, pour ne pas révéler son existence) : 404. Code {@code PAGE_INTROUVABLE}. */
public class PageIntrouvableException extends ExceptionIntrouvable {

    public static final String CODE = "PAGE_INTROUVABLE";

    public PageIntrouvableException() {
        super("Page introuvable.");
    }

    @Override
    public String code() {
        return CODE;
    }
}
