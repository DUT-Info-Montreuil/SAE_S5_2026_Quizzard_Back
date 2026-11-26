package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

/** Réponse de {@code GET /api/public/sessions/{codeAcces}} : de quoi afficher l'écran « Rejoindre » (E7). */
public record SessionPubliqueResponse(String titreCours, String titreQuiz, boolean estNotee, String etat) {
}
