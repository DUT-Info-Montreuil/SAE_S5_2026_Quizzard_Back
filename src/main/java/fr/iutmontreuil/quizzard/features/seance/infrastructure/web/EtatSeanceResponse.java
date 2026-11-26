package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Corps de {@code GET /api/public/sessions/{codeAcces}/etat}.
 *
 * @param question    question ouverte sans réponses attendues, ou {@code null}
 * @param dejaRepondu vrai si l'étudiant a déjà répondu à la question ouverte
 * @param scoreTotal  score de l'étudiant dans la séance
 */
record EtatSeanceResponse(QuestionResponse question, boolean dejaRepondu, int scoreTotal) {

    /**
     * Question pour l'étudiant. Champs propres au type : {@code propositions} et {@code choixMultiple} (QCM),
     * {@code elementsGauche} et {@code elementsDroite} (points à relier) ; absents pour les autres types.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record QuestionResponse(
            Long idQuestion,
            int position,
            String type,
            String enonce,
            int dureeSec,
            OffsetDateTime echeance,
            List<PropositionResponse> propositions,
            Boolean choixMultiple,
            List<ElementResponse> elementsGauche,
            List<ElementResponse> elementsDroite) {
    }

    record PropositionResponse(Long idProposition, String libelle) {
    }

    /** Élément à relier, désigné par l'identifiant de sa paire. */
    record ElementResponse(Long id, String libelle) {
    }
}
