package fr.iutmontreuil.quizzard.shared.domain;

/**
 * Catégorie : la ressource demandée n'existe pas (cours, quiz, session…). Traduite en 404.
 */
public abstract class ExceptionIntrouvable extends ExceptionMetier {

    protected ExceptionIntrouvable(String message) {
        super(message);
    }
}
