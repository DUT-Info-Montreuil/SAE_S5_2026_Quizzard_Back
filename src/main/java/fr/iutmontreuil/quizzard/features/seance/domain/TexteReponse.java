package fr.iutmontreuil.quizzard.features.seance.domain;

import java.util.Locale;

/** Normalisation d'une réponse libre avant toute comparaison (DESIGN.md §5). */
public final class TexteReponse {

    /** Longueur maximale d'une réponse libre (UC9, contrat du message). */
    public static final int LONGUEUR_MAX = 1000;

    private TexteReponse() {
    }

    /**
     * Espaces de début et de fin retirés, espaces multiples réduits à un seul, minuscules ; accents conservés
     * (à valider avec le client).
     */
    public static String normaliser(String texte) {
        return texte.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
