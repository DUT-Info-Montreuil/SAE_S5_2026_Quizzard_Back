package fr.iutmontreuil.quizzard.features.seance.domain;

/** Motif accepté pour une réponse libre (DESIGN.md §5). */
public record Motif(String motif, ModeMotif mode) {

    /** Longueur maximale d'un motif (DESIGN.md §5, protection du serveur). */
    public static final int LONGUEUR_MAX = 500;
}
