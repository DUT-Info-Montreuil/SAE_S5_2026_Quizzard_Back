package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflit;

/** Le participant a déjà une réponse pour cette question : 409. Code {@code DEJA_REPONDU} (UC9, tableau des cas d'erreur). */
public class DejaReponduException extends ExceptionConflit {

    public static final String CODE = "DEJA_REPONDU";

    public DejaReponduException() {
        super("Tu as déjà répondu.");
    }

    @Override
    public String code() {
        return CODE;
    }
}
