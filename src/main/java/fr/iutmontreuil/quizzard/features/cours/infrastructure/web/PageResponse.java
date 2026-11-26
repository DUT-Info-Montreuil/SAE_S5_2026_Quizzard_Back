package fr.iutmontreuil.quizzard.features.cours.infrastructure.web;

import java.util.List;

import fr.iutmontreuil.quizzard.features.cours.domain.PageInseree;

/**
 * Page insérée telle que l'éditeur E3 l'affiche dans la liste des pages : même forme dans les réponses de
 * l'insertion, de la lecture et du déplacement. {@code idQuestion}, {@code idQuiz} et {@code titreQuiz} sont nuls
 * pour une page de classement.
 */
record PageResponse(Long idPage, int numeroPage, int ordre, String type, Long idQuestion, Long idQuiz,
                    String titreQuiz) {

    static PageResponse de(PageInseree p) {
        return new PageResponse(p.idPage(), p.numeroPage(), p.ordre(), p.type().name(), p.idQuestion(), p.idQuiz(),
                p.titreQuiz());
    }

    static List<PageResponse> de(List<PageInseree> pages) {
        return pages.stream().map(PageResponse::de).toList();
    }
}
