package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionDonneesInvalides;

/** Contenu de la réponse incohérent avec la question : 422. Code {@code REPONSE_INVALIDE} (UC9, tableau des cas d'erreur). */
public class ReponseInvalideException extends ExceptionDonneesInvalides {

    public static final String CODE = "REPONSE_INVALIDE";

    public ReponseInvalideException() {
        super("Réponse incomplète, vérifie ta saisie.");
    }

    @Override
    public String code() {
        return CODE;
    }
}
