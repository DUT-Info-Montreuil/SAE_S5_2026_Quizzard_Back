package fr.iutmontreuil.quizzard.features.seance.application.dto;

/** Séance non terminée d'un enseignant : son identifiant et le cours présenté. */
public record SeanceOuverte(Long idSession, String titreCours) {
}
