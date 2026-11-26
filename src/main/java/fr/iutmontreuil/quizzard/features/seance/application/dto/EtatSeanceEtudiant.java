package fr.iutmontreuil.quizzard.features.seance.application.dto;

/**
 * État de la séance pour un étudiant, lu au chargement de l'écran et après une reconnexion.
 *
 * @param question    question ouverte, ou {@code null} s'il n'y en a pas
 * @param dejaRepondu vrai si l'étudiant a déjà répondu à la question ouverte
 * @param scoreTotal  score de l'étudiant dans la séance
 */
public record EtatSeanceEtudiant(QuestionPourEtudiant question, boolean dejaRepondu, int scoreTotal) {
}
