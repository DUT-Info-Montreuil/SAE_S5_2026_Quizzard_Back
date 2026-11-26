package fr.iutmontreuil.quizzard.features.seance.domain;

import java.time.LocalDateTime;

/**
 * Score d'un participant au moment de la clôture d'une question.
 *
 * @param tempsTotalMs    somme des temps de toutes ses réponses de la séance
 * @param aRepondu        vrai s'il a répondu à la question qui se clôt
 * @param pointsQuestion  points obtenus à cette question (0 sans réponse)
 * @param tempsQuestionMs temps de sa réponse à cette question (0 sans réponse)
 */
public record ScoreParticipant(
        Long idParticipant,
        int scoreTotal,
        long tempsTotalMs,
        LocalDateTime dateEntree,
        boolean aRepondu,
        int pointsQuestion,
        long tempsQuestionMs) {
}
