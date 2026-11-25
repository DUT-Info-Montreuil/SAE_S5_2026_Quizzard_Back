package fr.iutmontreuil.quizzard.features.seance.infrastructure.tempsreel;

/**
 * Le canal temps réel d'une séance ne peut pas être ouvert : le lancement est annulé et
 * {@code GestionnaireErreurs} répond 500 avec son message générique.
 */
public class CanalTempsReelException extends RuntimeException {

    public CanalTempsReelException() {
        super("Le canal temps réel de la séance ne peut pas être ouvert.");
    }
}
