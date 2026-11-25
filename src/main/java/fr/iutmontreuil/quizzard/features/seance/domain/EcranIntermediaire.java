package fr.iutmontreuil.quizzard.features.seance.domain;

/**
 * Écran à projeter après la clôture d'une question, selon la configuration du quiz (UC14).
 *
 * @param rang                 ordre d'affichage parmi les écrans de la question
 * @param delaiApresClotureSec secondes entre la clôture de la question et l'affichage
 */
public record EcranIntermediaire(TypeEcran type, int rang, int delaiApresClotureSec) {
}
