package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflit;

/** Quiz noté : l'entrée avec un simple pseudonyme est refusée, l'étudiant doit se connecter avec son compte IUT (409). */
public class ConnexionIutRequiseException extends ExceptionConflit {

    public ConnexionIutRequiseException() {
        super("Ce quiz est noté : connecte-toi avec ton compte IUT.");
    }
}
