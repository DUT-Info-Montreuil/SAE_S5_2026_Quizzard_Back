package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import fr.iutmontreuil.quizzard.features.seance.infrastructure.format.CorrectionJson;
import fr.iutmontreuil.quizzard.features.seance.infrastructure.format.QuestionJson;
import fr.iutmontreuil.quizzard.features.seance.infrastructure.format.RepartitionJson;

/**
 * État d'une séance pour l'écran E5. Les champs sans valeur sont absents : lien et QR code d'une séance terminée,
 * question avant la première, temps restant d'une question close, correction d'une question ouverte.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record DeroulementResponse(
        Long idSession,
        Long idQuiz,
        String titre,
        String statut,
        boolean modeNote,
        int nbQuestions,
        int position,
        String codeAcces,
        String lienAcces,
        String qrCode,
        OffsetDateTime dateLancement,
        OffsetDateTime dateCloture,
        int connectes,
        boolean questionOuverte,
        QuestionJson question,
        Long tempsRestantMs,
        RepartitionJson repartition,
        CorrectionJson correction) {

    @Override
    public String toString() {
        // Jamais le code d'accès dans un log.
        return "DeroulementResponse[idSession=" + idSession + ", statut=" + statut + ", position=" + position + "]";
    }
}
