package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionNonAuthentifie;

/** Jeton de participant absent, inconnu, remplacé ou d'une autre séance : 401. Code {@code JETON_INVALIDE} (UC9, tableau des cas d'erreur). */
public class ParticipantInvalideException extends ExceptionNonAuthentifie {

    public static final String CODE = "JETON_INVALIDE";

    public ParticipantInvalideException() {
        super("Ta session a expiré, rejoins le quiz à nouveau.");
    }

    @Override
    public String code() {
        return CODE;
    }
}
