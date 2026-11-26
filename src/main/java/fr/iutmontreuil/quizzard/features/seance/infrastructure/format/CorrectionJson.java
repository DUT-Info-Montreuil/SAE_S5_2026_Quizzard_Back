package fr.iutmontreuil.quizzard.features.seance.infrastructure.format;

import java.util.List;

/**
 * Correction d'une question close. Les listes qui ne concernent pas le type de la question sont vides.
 *
 * @param bonnesPropositions identifiants des propositions correctes (QCM)
 * @param paires             associations attendues (points à relier)
 * @param reponsesAcceptees  motifs simples d'une réponse libre
 * @param total              nombre de réponses reçues
 * @param repartition        nombre de réponses par proposition (QCM)
 */
public record CorrectionJson(
        Long idQuestion,
        int position,
        String type,
        List<Long> bonnesPropositions,
        List<Association> paires,
        List<String> reponsesAcceptees,
        int total,
        List<RepartitionJson.ParProposition> repartition) {

    public record Association(Long gauche, Long droite, String libelleGauche, String libelleDroite) {
    }
}
