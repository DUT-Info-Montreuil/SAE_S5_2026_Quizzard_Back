package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Corps de {@code POST /api/public/sessions/{codeAcces}/reponses}. Un seul des trois contenus sert, selon le type
 * de la question ; sa cohérence est vérifiée par le domaine (422 REPONSE_INVALIDE). Ni horodatage ni participant :
 * le serveur horodate la réponse et lit le participant dans le jeton.
 */
record RepondreQuestionRequete(
        @NotNull(message = "La question est obligatoire.") Long idQuestion,
        List<Long> idPropositions,
        List<@Valid LienRequete> liens,
        String texteLibre) {

    /** Identifiants des paires dont l'étudiant a choisi l'élément de gauche et l'élément de droite. */
    record LienRequete(Long gauche, Long droite) {
    }
}
