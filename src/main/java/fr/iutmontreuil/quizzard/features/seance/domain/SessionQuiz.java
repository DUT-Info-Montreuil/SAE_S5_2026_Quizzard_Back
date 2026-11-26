package fr.iutmontreuil.quizzard.features.seance.domain;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Agrégat de la séance, vu depuis une réponse d'étudiant (UC9). La question ouverte est celle de la position
 * courante ; elle n'est ouverte que si la séance est en cours et qu'une échéance est fixée.
 *
 * @param idQuestionOuverte question de la position courante, ou {@code null} s'il n'y en a pas
 * @param echeanceQuestion  échéance de la question ouverte (horloge du serveur), ou {@code null}
 */
public record SessionQuiz(
        Long idSession,
        String codeAcces,
        EtatSession etat,
        boolean estNotee,
        int positionCourante,
        Long idQuestionOuverte,
        LocalDateTime echeanceQuestion) {

    /** Vrai si une question attend des réponses (avant ou après son échéance). */
    public boolean aUneQuestionOuverte() {
        return etat == EtatSession.EN_COURS && idQuestionOuverte != null && echeanceQuestion != null;
    }

    /**
     * Vérifie que la séance est en cours, que la question visée est la question ouverte et que l'échéance n'est
     * pas dépassée, puis calcule le temps de réponse : durée de la question moins le temps restant.
     *
     * @return temps de réponse en millisecondes depuis l'ouverture de la question, borné entre 0 et la durée
     * @throws QuestionCloseException si la séance n'est pas EN_COURS ou si idQuestion n'est pas la question ouverte
     * @throws HorsDelaiException     si maintenant dépasse l'échéance
     */
    public long verifierReponseAcceptee(Long idQuestion, int dureeSec, LocalDateTime maintenant) {
        if (!aUneQuestionOuverte() || !idQuestionOuverte.equals(idQuestion)) {
            throw new QuestionCloseException();
        }
        if (maintenant.isAfter(echeanceQuestion)) {
            throw new HorsDelaiException();
        }
        long dureeMs = dureeSec * 1000L;
        long restantMs = Duration.between(maintenant, echeanceQuestion).toMillis();
        return Math.clamp(dureeMs - restantMs, 0, dureeMs);
    }
}
