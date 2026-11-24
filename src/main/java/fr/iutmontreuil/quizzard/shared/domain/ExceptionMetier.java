package fr.iutmontreuil.quizzard.shared.domain;

/**
 * Mère de toutes les exceptions métier. Elle ne connaît pas HTTP : c'est le gestionnaire d'erreurs
 * (couche infrastructure) qui choisit le statut, d'après la catégorie dont hérite l'exception.
 * <p>
 * Chaque feature crée ses propres exceptions nommées (ex. {@code CoursIntrouvableException}) en héritant
 * d'une catégorie de ce package. Le message est montré au client et écrit dans les logs : il ne contient
 * jamais de donnée personnelle (pas de nom, d'e-mail, de pseudonyme), seulement des identifiants.
 * <p>
 * Une exception peut aussi porter un code stable, en majuscules ({@code DEJA_REPONDU}), renvoyé au client dans le
 * champ {@code code} de l'erreur : le front choisit son message d'après ce code quand le statut HTTP ne suffit pas
 * (plusieurs causes de 409, par exemple). Sans code, le champ est absent.
 */
public abstract class ExceptionMetier extends RuntimeException {

    protected ExceptionMetier(String message) {
        super(message);
    }

    /** Code de l'erreur pour le client (exemple : {@code HORS_DELAI}), ou {@code null} s'il n'y en a pas. */
    public String code() {
        return null;
    }
}
