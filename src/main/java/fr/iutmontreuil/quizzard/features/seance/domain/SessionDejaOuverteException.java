package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflit;

/**
 * L'enseignant a déjà une séance non terminée : 409 {@code SESSION_DEJA_OUVERTE}. Le front propose de la reprendre
 * ou de la terminer (route {@code GET /api/sessions/ouverte}).
 */
public class SessionDejaOuverteException extends ExceptionConflit {

    public SessionDejaOuverteException(String titreCours) {
        super("Une session est déjà ouverte pour le cours « " + titreCours + " ». La reprendre ou la terminer ?");
    }

    @Override
    public String code() {
        return "SESSION_DEJA_OUVERTE";
    }
}
