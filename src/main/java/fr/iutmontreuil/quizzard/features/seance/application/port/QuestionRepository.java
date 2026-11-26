package fr.iutmontreuil.quizzard.features.seance.application.port;

import java.util.Optional;

import fr.iutmontreuil.quizzard.features.seance.domain.Question;

/**
 * Lecture d'une question avec son contenu attendu (propositions, paires, motifs) et sa durée effective (celle de la
 * question, sinon la durée par défaut du quiz).
 */
public interface QuestionRepository {

    Optional<Question> trouverParId(Long idQuestion);
}
