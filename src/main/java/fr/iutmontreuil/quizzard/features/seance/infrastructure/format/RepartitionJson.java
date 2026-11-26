package fr.iutmontreuil.quizzard.features.seance.infrastructure.format;

import java.util.List;

/**
 * Réponses reçues à la question courante, pour l'histogramme de E5. Même forme que le message
 * {@code /topic/sessions/{codeAcces}/repartition} de l'UC9 : les propositions jamais cochées sont absentes.
 */
public record RepartitionJson(Long idQuestion, int total, List<ParProposition> propositions) {

    public record ParProposition(Long idProposition, int nombre) {
    }
}
