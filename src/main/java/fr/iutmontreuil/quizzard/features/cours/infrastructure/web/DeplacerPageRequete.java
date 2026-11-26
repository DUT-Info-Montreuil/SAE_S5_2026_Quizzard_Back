package fr.iutmontreuil.quizzard.features.cours.infrastructure.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Corps de {@code PATCH /api/pages/{idPage}} : nouvel emplacement, mêmes règles que l'insertion. */
record DeplacerPageRequete(
        @NotNull(message = "La page est obligatoire.")
        @PositiveOrZero(message = "La page doit être positive ou nulle.") Integer numeroPage,
        @NotNull(message = "Le rang est obligatoire.")
        @PositiveOrZero(message = "Le rang doit être positif ou nul.") Integer ordre) {
}
