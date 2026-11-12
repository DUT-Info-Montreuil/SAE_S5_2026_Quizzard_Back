package fr.iutmontreuil.quizzard.shared.domain;

/**
 * Catégorie : le type du fichier ou du contenu reçu n'est pas accepté (fichier non PDF…). Traduite en 415.
 */
public abstract class ExceptionTypeNonSupporte extends ExceptionMetier {

    protected ExceptionTypeNonSupporte(String message) {
        super(message);
    }
}
