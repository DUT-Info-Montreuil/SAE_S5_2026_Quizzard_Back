package fr.iutmontreuil.quizzard.features.cours.application;

/**
 * Le fichier ne peut pas être écrit ou rangé sur le disque. Ce n'est pas un refus métier : la transaction est annulée
 * et {@code GestionnaireErreurs} répond 500 avec son message générique.
 */
public class StockageFichierException extends RuntimeException {

    public StockageFichierException(String message, Throwable cause) {
        super(message, cause);
    }
}
