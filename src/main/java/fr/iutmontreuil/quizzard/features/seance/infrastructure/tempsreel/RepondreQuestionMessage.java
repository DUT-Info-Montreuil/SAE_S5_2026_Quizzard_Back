package fr.iutmontreuil.quizzard.features.seance.infrastructure.tempsreel;

import java.util.List;

import jakarta.validation.constraints.NotNull;

/**
 * Message {@code SEND /app/sessions/{idSession}/reponses} (dossier de conception, UC9). Même contenu que la route
 * REST, plus {@code requestId}, recopié dans l'accusé ou l'erreur pour que le client les relie à son envoi.
 */
record RepondreQuestionMessage(
        String requestId,
        @NotNull Long idQuestion,
        List<Long> idPropositions,
        List<LienMessage> liens,
        String texteLibre) {

    record LienMessage(Long gauche, Long droite) {
    }
}
