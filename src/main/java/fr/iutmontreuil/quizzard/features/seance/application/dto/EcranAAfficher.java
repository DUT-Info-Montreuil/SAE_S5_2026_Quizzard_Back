package fr.iutmontreuil.quizzard.features.seance.application.dto;

import fr.iutmontreuil.quizzard.features.seance.domain.EcranIntermediaire;

/** Écran intermédiaire à projeter maintenant, après la question close (événement {@code ecran_intermediaire}). */
public record EcranAAfficher(Long idQuestion, int position, EcranIntermediaire ecran) {
}
