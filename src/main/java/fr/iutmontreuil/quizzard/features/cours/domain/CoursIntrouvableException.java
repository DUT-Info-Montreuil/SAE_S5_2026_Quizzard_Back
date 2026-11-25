package fr.iutmontreuil.quizzard.features.cours.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionIntrouvable;

/** Cours inexistant ou d'un autre enseignant (même réponse, pour ne pas révéler son existence) : 404. Code {@code COURS_INTROUVABLE} (UC3, tableau des cas d'erreur). */
public class CoursIntrouvableException extends ExceptionIntrouvable {

    public static final String CODE = "COURS_INTROUVABLE";

    public CoursIntrouvableException() {
        super("Cours introuvable.");
    }

    @Override
    public String code() {
        return CODE;
    }
}
