package fr.iutmontreuil.quizzard.shared.domain;

/**
 * Catégorie : l'opération demande d'être connecté. Traduite en 401.
 */
public abstract class ExceptionNonAuthentifie extends ExceptionMetier {

    protected ExceptionNonAuthentifie(String message) {
        super(message);
    }
}
