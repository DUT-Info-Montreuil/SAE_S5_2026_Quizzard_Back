package fr.iutmontreuil.quizzard.shared.domain;

/**
 * Catégorie : l'utilisateur est connecté mais n'a pas le droit (cours d'un autre enseignant…). Traduite en 403.
 */
public abstract class ExceptionAccesRefuse extends ExceptionMetier {

    protected ExceptionAccesRefuse(String message) {
        super(message);
    }
}
