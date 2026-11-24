package fr.iutmontreuil.quizzard.features.cours.infrastructure.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Corps de {@code POST /api/cours/{idCours}/pages-quiz} (UC3). Un champ absent ou négatif, ou un repère trop long,
 * donne 400 REQUETE_INVALIDE.
 *
 * @param idQuiz      quiz existant du cours, ou absent pour créer un nouveau quiz
 * @param repereTexte texte de la page qui précède, extrait par PDF.js (300 caractères au plus)
 */
record InsererPageQuizRequete(
        @NotNull(message = "La version est obligatoire.") Long idVersion,
        @NotNull(message = "La page est obligatoire.")
        @PositiveOrZero(message = "La page doit être positive ou nulle.") Integer numeroPage,
        @NotNull(message = "Le rang est obligatoire.")
        @PositiveOrZero(message = "Le rang doit être positif ou nul.") Integer ordre,
        Long idQuiz,
        @Size(max = 300, message = "Le repère fait 300 caractères au plus.") String repereTexte) {
}
