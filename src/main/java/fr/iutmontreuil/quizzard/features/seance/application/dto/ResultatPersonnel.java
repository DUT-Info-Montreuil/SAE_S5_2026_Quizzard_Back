package fr.iutmontreuil.quizzard.features.seance.application.dto;

import java.util.List;

/**
 * Résultat d'une question envoyé au seul étudiant concerné, après la clôture.
 *
 * @param points             points de cette question (provisoires pour une réponse libre)
 * @param evolution          places gagnées (positif) ou perdues (négatif) depuis la question précédente
 * @param repondu            vrai si l'étudiant avait répondu à temps
 * @param bonneReponse       la bonne réponse en texte
 */
public record ResultatPersonnel(
        Long idParticipant,
        Long idQuestion,
        int points,
        int scoreTotal,
        int rang,
        int nombreParticipants,
        int evolution,
        boolean repondu,
        List<String> bonneReponse) {
}
