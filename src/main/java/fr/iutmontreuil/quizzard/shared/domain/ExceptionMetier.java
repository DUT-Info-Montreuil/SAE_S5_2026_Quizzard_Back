package fr.iutmontreuil.quizzard.shared.domain;

/**
 * Mère de toutes les exceptions métier. Elle ne connaît pas HTTP : c'est le gestionnaire d'erreurs
 * (couche infrastructure) qui choisit le statut, d'après la catégorie dont hérite l'exception.
 * <p>
 * Chaque feature crée ses propres exceptions nommées (ex. {@code CoursIntrouvableException}) en héritant
 * d'une catégorie de ce package. Le message est montré au client et écrit dans les logs : il ne contient
 * jamais de donnée personnelle (pas de nom, d'e-mail, de pseudonyme), seulement des identifiants.
 */
public abstract class ExceptionMetier extends RuntimeException {

    protected ExceptionMetier(String message) {
        super(message);
    }
}
