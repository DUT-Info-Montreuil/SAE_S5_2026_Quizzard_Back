package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflit;

/** Réponse reçue après l'échéance de la question (horloge du serveur) : 409. Code {@code HORS_DELAI} (UC9, tableau des cas d'erreur). */
public class HorsDelaiException extends ExceptionConflit {

    public static final String CODE = "HORS_DELAI";

    public HorsDelaiException() {
        super("Temps écoulé : réponse non prise en compte.");
    }

    @Override
    public String code() {
        return CODE;
    }
}
