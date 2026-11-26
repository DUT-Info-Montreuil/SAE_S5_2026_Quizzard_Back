package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflit;

/** La question courante attend encore des réponses : 409 {@code QUESTION_DEJA_OUVERTE}. */
public class QuestionDejaOuverteException extends ExceptionConflit {

    public QuestionDejaOuverteException() {
        super("Une question est déjà ouverte. Clôturez-la avant de passer à la suivante.");
    }

    @Override
    public String code() {
        return "QUESTION_DEJA_OUVERTE";
    }
}
