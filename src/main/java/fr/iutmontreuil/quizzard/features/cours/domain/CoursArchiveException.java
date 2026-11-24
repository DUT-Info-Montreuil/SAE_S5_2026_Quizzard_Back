package fr.iutmontreuil.quizzard.features.cours.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflit;

/** Cours archivé : 409. Code {@code COURS_ARCHIVE} (UC3, tableau des cas d'erreur). */
public class CoursArchiveException extends ExceptionConflit {

    public static final String CODE = "COURS_ARCHIVE";

    public CoursArchiveException() {
        super("Ce cours est archivé : il n'est plus modifiable.");
    }

    @Override
    public String code() {
        return CODE;
    }
}
