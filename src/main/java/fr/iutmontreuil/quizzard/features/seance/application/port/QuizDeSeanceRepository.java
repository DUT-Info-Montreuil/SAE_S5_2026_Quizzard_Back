package fr.iutmontreuil.quizzard.features.seance.application.port;

import java.util.Collection;
import java.util.Optional;

import fr.iutmontreuil.quizzard.features.seance.domain.Quiz;

/**
 * Quiz vu par la séance : propriétaire, réglages et questions de la version du cours présentée. Port propre à la
 * feature {@code seance}, qui ne dépend ainsi pas de l'infrastructure des features {@code quiz} et {@code cours}.
 */
public interface QuizDeSeanceRepository {

    /** Quiz à lancer, avec les questions de la dernière version du cours. Un quiz archivé est introuvable. */
    Optional<Quiz> trouverParId(Long idQuiz);

    /** Quiz d'une séance déjà lancée, avec les questions de la version présentée, même archivé depuis. */
    Optional<Quiz> trouverPourSeance(Long idQuiz, Long idVersion);

    /** Verrouille les questions jouées : les modifier n'altérera pas les séances passées (DESIGN.md §4). */
    void verrouillerQuestions(Collection<Long> idQuestions);
}
