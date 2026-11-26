package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionIntrouvable;

/** Le quiz à lancer n'existe pas, ou il est archivé : 404. */
public class QuizIntrouvableException extends ExceptionIntrouvable {

    public QuizIntrouvableException() {
        super("Ce quiz n'existe pas ou a été supprimé.");
    }
}
