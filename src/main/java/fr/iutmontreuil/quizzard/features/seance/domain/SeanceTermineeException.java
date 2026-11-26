package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflit;

/** La séance est close, son historique est en lecture seule : 409 {@code SEANCE_TERMINEE}. */
public class SeanceTermineeException extends ExceptionConflit {

    public SeanceTermineeException() {
        super("Cette séance est terminée.");
    }

    @Override
    public String code() {
        return "SEANCE_TERMINEE";
    }
}
