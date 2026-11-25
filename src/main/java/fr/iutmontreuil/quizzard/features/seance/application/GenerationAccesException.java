package fr.iutmontreuil.quizzard.features.seance.application;

/**
 * Le jeton d'accès ou le QR code ne peut pas être produit. Ce n'est pas un refus métier : la transaction est
 * annulée et {@code GestionnaireErreurs} répond 500 avec son message générique.
 */
public class GenerationAccesException extends RuntimeException {

    public GenerationAccesException(String message, Throwable cause) {
        super(message, cause);
    }
}
