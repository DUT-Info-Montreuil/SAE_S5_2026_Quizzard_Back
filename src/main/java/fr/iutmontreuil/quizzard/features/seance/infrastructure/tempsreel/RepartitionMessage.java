package fr.iutmontreuil.quizzard.features.seance.infrastructure.tempsreel;

import java.util.List;

/**
 * Répartition en direct des réponses à la question ouverte, pour l'écran de l'enseignant (E5) :
 * {@code /topic/sessions/{codeAcces}/repartition}. Pour un QCM, le nombre de réponses par proposition ; pour les
 * autres types, seulement le total reçu.
 */
record RepartitionMessage(Long idQuestion, long total, List<ParProposition> propositions) {

    record ParProposition(Long idProposition, long nombre) {
    }
}
