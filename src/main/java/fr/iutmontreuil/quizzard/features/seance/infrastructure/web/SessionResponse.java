package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import java.time.OffsetDateTime;

/**
 * Séance créée par le lancement.
 *
 * @param codeAcces désigne les canaux temps réel de la séance ({@code /topic/sessions/{codeAcces}/…})
 * @param lienAcces adresse que les étudiants ouvrent
 * @param qrCode    image PNG du QR code de ce lien, encodée en base64
 */
record SessionResponse(
        Long idSession,
        Long idQuiz,
        String titre,
        String statut,
        boolean modeNote,
        int nbQuestions,
        String codeAcces,
        String lienAcces,
        String qrCode,
        OffsetDateTime dateLancement) {

    @Override
    public String toString() {
        // Jamais le code d'accès dans un log.
        return "SessionResponse[idSession=" + idSession + ", statut=" + statut + "]";
    }
}
