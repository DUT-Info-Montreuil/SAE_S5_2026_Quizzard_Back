package fr.iutmontreuil.quizzard.features.seance.domain;

/**
 * Statut de correction d'une réponse (DESIGN.md §5). Seules les réponses libres passent par les motifs ;
 * les autres types ont le statut SANS_OBJET.
 */
public enum StatutCorrection {
    SANS_OBJET,
    ACCEPTEE_PAR_MOTIF,
    A_EXAMINER,
    VALIDEE,
    REFUSEE
}
