package fr.iutmontreuil.quizzard.shared.domain;

/**
 * Catégorie : l'opération entre en conflit avec l'état actuel (doublon, question verrouillée…). Traduite en 409.
 */
public abstract class ExceptionConflit extends ExceptionMetier {

    protected ExceptionConflit(String message) {
        super(message);
    }
}
