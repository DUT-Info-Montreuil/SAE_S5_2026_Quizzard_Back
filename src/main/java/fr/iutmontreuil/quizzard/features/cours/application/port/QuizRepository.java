package fr.iutmontreuil.quizzard.features.cours.application.port;

import java.util.Optional;

import fr.iutmontreuil.quizzard.features.cours.domain.Quiz;

/** Accès aux quiz d'un cours. */
public interface QuizRepository {

    /** Quiz non archivé de ce cours ; vide s'il n'existe pas, est archivé ou appartient à un autre cours. */
    Optional<Quiz> trouverActif(Long idQuiz, Long idCours);

    /** Nombre de quiz du cours, archivés compris (numérote « Quiz n »). */
    int compterQuiz(Long idCours);

    /** Vrai si une séance de ce quiz est à l'état EN_COURS. */
    boolean estEnSeance(Long idQuiz);

    /** Enregistre le quiz s'il est nouveau, puis ses nouvelles questions ; renseigne leurs identifiants. */
    Quiz enregistrer(Quiz quiz);
}
