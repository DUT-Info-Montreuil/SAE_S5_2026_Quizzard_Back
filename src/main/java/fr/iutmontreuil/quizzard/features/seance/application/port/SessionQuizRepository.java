package fr.iutmontreuil.quizzard.features.seance.application.port;

import java.util.Optional;

import fr.iutmontreuil.quizzard.features.seance.domain.SessionQuiz;

/** Accès aux séances pour les routes des étudiants. */
public interface SessionQuizRepository {

    /**
     * Séance qui porte ce code d'accès, avec sa question ouverte : la page de quiz de rang {@code positionCourante}
     * dans la version du cours projetée (pages triées par page du PDF puis par ordre).
     */
    Optional<SessionQuiz> trouverParCodeAcces(String codeAcces);

    /** Même lecture, par identifiant de séance. */
    Optional<SessionQuiz> trouverParId(Long idSession);
}
