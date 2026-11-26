package fr.iutmontreuil.quizzard.features.seance.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.features.seance.application.port.DiffuseurSeance;
import fr.iutmontreuil.quizzard.features.seance.application.port.SeanceAnimeeRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.Quiz;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/**
 * Fin d'une séance, demandée par l'enseignant ou atteinte après la dernière question : la question encore ouverte est
 * close, puis la séance. Son code d'accès ne vaut plus rien et son historique est en lecture seule. S'exécute dans la
 * transaction du cas d'usage appelant.
 */
@Component
class FinDeSeance {

    private final FinDeQuestion finDeQuestion;
    private final SeanceAnimeeRepository seances;
    private final DiffuseurSeance diffuseur;
    private final JournalEvenements journal;
    private final Clock horloge;

    FinDeSeance(FinDeQuestion finDeQuestion, SeanceAnimeeRepository seances, DiffuseurSeance diffuseur,
                JournalEvenements journal, Clock horloge) {
        this.finDeQuestion = finDeQuestion;
        this.seances = seances;
        this.diffuseur = diffuseur;
        this.journal = journal;
        this.horloge = horloge;
    }

    /** @throws fr.iutmontreuil.quizzard.features.seance.domain.SeanceTermineeException si elle est déjà close */
    void terminer(SeanceAnimee seance, Quiz quiz) {
        seance.verifierNonTerminee();
        if (seance.aUneQuestionOuverte()) {
            finDeQuestion.clore(seance, quiz);
        }
        seance.cloturer(LocalDateTime.now(horloge));
        seances.mettreAJour(seance);
        diffuseur.annoncerFin(seance.codeAcces(), seance.positionCourante());
        journal.evenement("seance_cloturee", Map.of("idSession", seance.idSession()));
    }
}
