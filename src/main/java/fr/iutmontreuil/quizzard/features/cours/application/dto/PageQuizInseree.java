package fr.iutmontreuil.quizzard.features.cours.application.dto;

import java.util.List;

import fr.iutmontreuil.quizzard.features.cours.domain.PageInseree;
import fr.iutmontreuil.quizzard.features.cours.domain.PageQuiz;
import fr.iutmontreuil.quizzard.features.cours.domain.Quiz;

/**
 * Résultat de l'insertion : la page créée, son quiz (créé ou non), la question vierge et la liste à jour des pages
 * insérées de la version.
 */
public record PageQuizInseree(PageQuiz page, Quiz quiz, boolean quizCree, Long idQuestion, List<PageInseree> pages) {
}
