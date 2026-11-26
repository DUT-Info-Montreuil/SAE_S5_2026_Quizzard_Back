package fr.iutmontreuil.quizzard.features.cours.infrastructure.web;

import java.util.List;

import fr.iutmontreuil.quizzard.features.cours.domain.QuizDuCours;

/** Quiz du cours : {@code {idQuiz, titre}}, forme lue par le dialogue de choix de l'éditeur E3. */
record QuizDuCoursResponse(Long idQuiz, String titre) {

    static List<QuizDuCoursResponse> de(List<QuizDuCours> quiz) {
        return quiz.stream().map(q -> new QuizDuCoursResponse(q.idQuiz(), q.titre())).toList();
    }
}
