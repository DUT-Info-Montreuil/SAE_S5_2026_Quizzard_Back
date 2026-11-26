package fr.iutmontreuil.quizzard.features.seance.application.dto;

import java.util.Map;

/**
 * Réponses reçues pour une question dans une séance.
 *
 * @param total          nombre de réponses
 * @param parProposition nombre de réponses qui cochent chaque proposition ; les propositions jamais cochées sont
 *                       absentes (vide hors QCM)
 */
public record RepartitionReponses(int total, Map<Long, Integer> parProposition) {

    public RepartitionReponses {
        parProposition = Map.copyOf(parProposition);
    }
}
