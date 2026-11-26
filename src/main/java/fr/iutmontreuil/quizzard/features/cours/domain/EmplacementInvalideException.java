package fr.iutmontreuil.quizzard.features.cours.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionDonneesInvalides;

/** Emplacement hors du cours (page ou rang hors des bornes) : 422. Code {@code EMPLACEMENT_INVALIDE} (UC3, tableau des cas d'erreur). */
public class EmplacementInvalideException extends ExceptionDonneesInvalides {

    public static final String CODE = "EMPLACEMENT_INVALIDE";

    public EmplacementInvalideException() {
        super("Emplacement invalide : rechargez l'éditeur.");
    }

    @Override
    public String code() {
        return CODE;
    }
}
