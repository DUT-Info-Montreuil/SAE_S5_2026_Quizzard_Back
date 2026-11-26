package fr.iutmontreuil.quizzard.features.seance.domain;

/**
 * Motif accepté pour une réponse libre jouée en séance. Seuls les motifs simples sont lisibles par un étudiant : ils
 * sont montrés comme réponses acceptées après la clôture.
 */
public record MotifDeSeance(String motif, boolean expressionReguliere) {
}
