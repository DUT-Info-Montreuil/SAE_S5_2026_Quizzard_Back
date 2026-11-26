package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionAccesRefuse;

/** Le quiz appartient à un autre enseignant : 403, sans rien révéler du quiz. */
public class AccesQuizRefuseException extends ExceptionAccesRefuse {

    public AccesQuizRefuseException() {
        super("Action non autorisée.");
    }
}
