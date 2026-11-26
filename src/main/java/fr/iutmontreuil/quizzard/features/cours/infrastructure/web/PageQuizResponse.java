package fr.iutmontreuil.quizzard.features.cours.infrastructure.web;

import java.util.List;

import fr.iutmontreuil.quizzard.features.cours.application.dto.PageQuizInseree;

/**
 * Réponse 201 de l'insertion : la page créée, son quiz, la question vierge et la liste à jour des pages insérées
 * de la version (pour que l'éditeur E3 recalcule sa numérotation sans relire le cours).
 */
record PageQuizResponse(
        Long idPage,
        Long idQuiz,
        String titreQuiz,
        boolean quizCree,
        Long idQuestion,
        int numeroPage,
        int ordre,
        List<PageResponse> pages) {

    static PageQuizResponse de(PageQuizInseree r) {
        return new PageQuizResponse(r.page().idPage(), r.quiz().idQuiz(), r.quiz().titre(), r.quizCree(),
                r.idQuestion(), r.page().numeroPage(), r.page().ordre(),
                PageResponse.de(r.pages()));
    }
}
