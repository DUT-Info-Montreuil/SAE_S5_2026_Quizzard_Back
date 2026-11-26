package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionIntrouvable;

/** Aucune séance ne porte ce code d'accès : 404. Le code n'est pas recopié dans le message. */
public class SessionIntrouvableException extends ExceptionIntrouvable {

    public SessionIntrouvableException() {
        super("Ce quiz est introuvable.");
    }
}
