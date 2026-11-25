package fr.iutmontreuil.quizzard.features.seance.infrastructure.planification;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import fr.iutmontreuil.quizzard.features.seance.application.CloreQuestionUseCase;
import fr.iutmontreuil.quizzard.features.seance.application.DemarrerQuestionUseCase;
import fr.iutmontreuil.quizzard.features.seance.application.port.MinuterieSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.EcranIntermediaire;
import jakarta.annotation.PreDestroy;

/**
 * Minuterie en mémoire, sur le planificateur de Spring (ARCHITECTURE.md §3.2) : une tâche par échéance. Le
 * planificateur est propre à la séance, pour ne pas partager ses fils avec le canal temps réel. Les cas d'usage sont
 * obtenus à l'exécution, car ils dépendent eux-mêmes de la minuterie.
 */
@Component
class MinuterieSpring implements MinuterieSeance {

    /**
     * Attente après l'échéance avant la clôture : une réponse reçue juste à temps a fini d'être enregistrée. Une
     * réponse reçue après l'échéance est refusée de toute façon.
     */
    static final Duration MARGE_CLOTURE = Duration.ofMillis(250);

    private static final Logger journal = LoggerFactory.getLogger("metier");

    private final ObjectProvider<CloreQuestionUseCase> cloreQuestion;
    private final ObjectProvider<DemarrerQuestionUseCase> demarrerQuestion;
    private final Clock horloge;
    private final ThreadPoolTaskScheduler planificateur = new ThreadPoolTaskScheduler();

    MinuterieSpring(ObjectProvider<CloreQuestionUseCase> cloreQuestion,
                    ObjectProvider<DemarrerQuestionUseCase> demarrerQuestion, Clock horloge) {
        this.cloreQuestion = cloreQuestion;
        this.demarrerQuestion = demarrerQuestion;
        this.horloge = horloge;
        planificateur.setPoolSize(2);
        planificateur.setThreadNamePrefix("minuterie-seance-");
        planificateur.initialize();
    }

    @Override
    public void programmerCloture(Long idSession, int position, LocalDateTime echeance) {
        programmer("cloture", idSession, echeance.plus(MARGE_CLOTURE),
                () -> cloreQuestion.getObject().executerAEcheance(idSession, position));
    }

    @Override
    public void programmerEcran(Long idSession, int position, EcranIntermediaire ecran, LocalDateTime moment) {
        programmer("ecran", idSession, moment,
                () -> cloreQuestion.getObject().afficherEcran(idSession, position, ecran));
    }

    @Override
    public void programmerQuestionSuivante(Long idSession, int position, LocalDateTime moment) {
        programmer("passage_automatique", idSession, moment,
                () -> demarrerQuestion.getObject().executerAutomatiquement(idSession, position));
    }

    private void programmer(String tache, Long idSession, LocalDateTime moment, Runnable action) {
        Runnable programmation = () -> planificateur.schedule(() -> executer(tache, idSession, action),
                moment.atZone(horloge.getZone()).toInstant());
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            programmation.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                programmation.run();
            }
        });
    }

    /** Un échec n'arrête pas la minuterie : l'enseignant garde la main avec ses commandes. */
    private static void executer(String tache, Long idSession, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            journal.atWarn().addKeyValue("tache", tache).addKeyValue("idSession", idSession)
                    .addKeyValue("exception", e.getClass().getSimpleName()).log("tache_seance_echouee");
        }
    }

    @PreDestroy
    void arreter() {
        planificateur.shutdown();
    }
}
