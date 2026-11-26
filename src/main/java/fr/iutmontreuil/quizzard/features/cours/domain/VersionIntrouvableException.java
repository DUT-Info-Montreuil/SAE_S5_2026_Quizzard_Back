package fr.iutmontreuil.quizzard.features.cours.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionIntrouvable;

/** Version qui n'appartient pas au cours : 404. Code {@code VERSION_INTROUVABLE}. */
public class VersionIntrouvableException extends ExceptionIntrouvable {

    public static final String CODE = "VERSION_INTROUVABLE";

    public VersionIntrouvableException() {
        super("Version introuvable.");
    }

    @Override
    public String code() {
        return CODE;
    }
}
