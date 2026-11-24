package fr.iutmontreuil.quizzard.features.cours.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflit;

/** La version visée n'est pas la dernière version du cours : 409. Code {@code VERSION_PERIMEE} (UC3, tableau des cas d'erreur). */
public class VersionPerimeeException extends ExceptionConflit {

    public static final String CODE = "VERSION_PERIMEE";

    public VersionPerimeeException() {
        super("Une nouvelle version du cours a été importée : rechargez l'éditeur.");
    }

    @Override
    public String code() {
        return CODE;
    }
}
