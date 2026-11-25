package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflitAvecSuggestion;

/**
 * Un participant de la séance porte déjà ce pseudonyme (409).
 * <ul>
 *   <li>Levée par le cas d'usage : elle porte un pseudonyme libre à proposer à l'étudiant.</li>
 *   <li>Levée par le dépôt quand la contrainte d'unicité de la base est violée (deux étudiants envoient le même
 *       pseudonyme au même instant) : aucune proposition, le second est simplement refusé.</li>
 * </ul>
 * Le pseudonyme proposé n'est pas dans le message (écrit dans les logs) : il voyage dans le champ {@code suggestion}.
 */
public class PseudonymeDejaPrisException extends ExceptionConflitAvecSuggestion {

    /** Sans proposition (doublon détecté par la base). */
    public PseudonymeDejaPrisException() {
        super("Ce pseudonyme est déjà pris.", null);
    }

    /** Avec un pseudonyme libre proposé à l'étudiant. */
    public PseudonymeDejaPrisException(Pseudonyme suggestion) {
        super("Ce pseudonyme est déjà pris. Essaie celui qui t'est proposé.", suggestion.valeur());
    }
}
