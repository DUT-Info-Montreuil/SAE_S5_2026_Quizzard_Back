package fr.iutmontreuil.quizzard.shared.domain;

/**
 * Catégorie : conflit pour lequel une valeur de remplacement peut être proposée au client (par exemple un
 * pseudonyme libre). Traduite en 409, avec le champ {@code suggestion} quand une valeur est disponible.
 * <p>
 * La suggestion n'est jamais recopiée dans le message de l'exception, qui est écrit dans les logs.
 */
public abstract class ExceptionConflitAvecSuggestion extends ExceptionConflit {

    private final String suggestion;

    protected ExceptionConflitAvecSuggestion(String message, String suggestion) {
        super(message);
        this.suggestion = suggestion;
    }

    /** Valeur proposée, ou {@code null} si aucune n'est disponible. */
    public String suggestion() {
        return suggestion;
    }
}
