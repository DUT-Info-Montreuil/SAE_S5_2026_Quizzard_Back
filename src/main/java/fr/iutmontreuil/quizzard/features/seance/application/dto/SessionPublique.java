package fr.iutmontreuil.quizzard.features.seance.application.dto;

import fr.iutmontreuil.quizzard.features.seance.domain.EtatSession;

/** Ce qu'un étudiant voit d'une séance avant d'entrer : titres, mode noté et état. */
public record SessionPublique(String titreCours, String titreQuiz, boolean estNotee, EtatSession etat) {
}
