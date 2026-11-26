package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionAccesRefuse;

/** La séance appartient à un autre enseignant : 403. */
public class AccesSeanceRefuseException extends ExceptionAccesRefuse {

    public AccesSeanceRefuseException() {
        super("Action non autorisée.");
    }
}
