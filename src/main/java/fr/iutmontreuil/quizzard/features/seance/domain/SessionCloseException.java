package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflit;

/** La séance est close (état {@code CLOTUREE}) : elle n'accepte plus aucune entrée (409). */
public class SessionCloseException extends ExceptionConflit {

    public SessionCloseException() {
        super("Ce quiz est terminé.");
    }
}
