package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps de {@code POST /api/public/sessions/{codeAcces}/participants}. Le contrôle de forme évite un aller-retour
 * inutile ; le domaine ({@code Pseudonyme.de}) refait le contrôle après nettoyage et reste le dernier rempart.
 */
record RejoindreSessionRequest(
        @NotBlank(message = MESSAGE_PSEUDONYME)
        @Size(min = 2, max = 20, message = MESSAGE_PSEUDONYME)
        String pseudonyme) {

    static final String MESSAGE_PSEUDONYME = "Ton pseudonyme doit faire de 2 à 20 caractères.";
}
