package fr.iutmontreuil.quizzard.shared.domain;

/**
 * Catégorie : une règle métier refuse les données reçues (titre vide, durée négative…). Traduite en 422.
 * Le champ fautif, s'il est connu, est renvoyé au client pour qu'il l'affiche à côté du bon champ.
 */
public abstract class ExceptionDonneesInvalides extends ExceptionMetier {

    private final String champ;

    protected ExceptionDonneesInvalides(String message) {
        this(null, message);
    }

    protected ExceptionDonneesInvalides(String champ, String message) {
        super(message);
        this.champ = champ;
    }

    /** Nom du champ fautif, ou {@code null} si l'erreur ne porte pas sur un champ précis. */
    public String champ() {
        return champ;
    }
}
