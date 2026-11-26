package fr.iutmontreuil.quizzard.features.seance.application;

import java.util.Optional;

import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.features.seance.application.port.QuizDeSeanceRepository;
import fr.iutmontreuil.quizzard.features.seance.application.port.SeanceAnimeeRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.Quiz;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceIntrouvableException;

/**
 * Lecture d'une séance pour les commandes du déroulement : chaque commande de l'enseignant vérifie que la séance lui
 * appartient.
 */
@Component
class AccesSeance {

    private final SeanceAnimeeRepository seances;
    private final QuizDeSeanceRepository quizzes;

    AccesSeance(SeanceAnimeeRepository seances, QuizDeSeanceRepository quizzes) {
        this.seances = seances;
        this.quizzes = quizzes;
    }

    /** Séance de cet enseignant, verrouillée jusqu'à la fin de la transaction. */
    SeanceAnimee pourModification(Long idSession, Long idEnseignant) {
        return verifiee(seances.trouverPourModification(idSession), idEnseignant);
    }

    /** Séance de cet enseignant, sans verrou. */
    SeanceAnimee pourLecture(Long idSession, Long idEnseignant) {
        return verifiee(seances.trouverParId(idSession), idEnseignant);
    }

    /** Quiz joué dans la séance, avec les questions de la version présentée. */
    Quiz quizDe(SeanceAnimee seance) {
        return quizzes.trouverPourSeance(seance.idQuiz(), seance.idVersion())
                // Les clés étrangères garantissent le quiz d'une séance existante.
                .orElseThrow(() -> new IllegalStateException("Quiz introuvable pour la séance " + seance.idSession()));
    }

    private static SeanceAnimee verifiee(Optional<SeanceAnimee> seance, Long idEnseignant) {
        SeanceAnimee trouvee = seance.orElseThrow(SeanceIntrouvableException::new);
        trouvee.verifierProprietaire(idEnseignant);
        return trouvee;
    }
}
