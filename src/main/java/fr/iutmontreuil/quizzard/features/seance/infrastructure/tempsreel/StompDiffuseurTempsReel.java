package fr.iutmontreuil.quizzard.features.seance.infrastructure.tempsreel;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import fr.iutmontreuil.quizzard.features.seance.application.port.DiffuseurTempsReel;
import fr.iutmontreuil.quizzard.features.seance.infrastructure.tempsreel.RepartitionMessage.ParProposition;
import jakarta.annotation.PreDestroy;

/**
 * Diffusion STOMP des événements de la séance. La répartition est envoyée après la validation de la transaction
 * (une réponse annulée n'est jamais annoncée) et regroupée : au plus un envoi toutes les 300 ms par séance et par
 * question (API.md §4), avec les nombres à jour au moment de l'envoi.
 */
@Component
class StompDiffuseurTempsReel implements DiffuseurTempsReel {

    static final long DELAI_REGROUPEMENT_MS = 300;
    private static final Logger journal = LoggerFactory.getLogger(StompDiffuseurTempsReel.class);

    private final SimpMessagingTemplate messages;
    private final JdbcClient jdbc;
    private final ScheduledExecutorService planificateur = Executors.newSingleThreadScheduledExecutor();
    private final Set<String> envoisPrevus = ConcurrentHashMap.newKeySet();

    StompDiffuseurTempsReel(SimpMessagingTemplate messages, JdbcClient jdbc) {
        this.messages = messages;
        this.jdbc = jdbc;
    }

    @Override
    public void signalerNouvelleReponse(String codeAcces, Long idQuestion) {
        apresValidation(() -> planifierRepartition(codeAcces, idQuestion));
    }

    private static void apresValidation(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    /** Un seul envoi prévu à la fois par séance et par question : les réponses arrivées entre-temps s'y ajoutent. */
    private void planifierRepartition(String codeAcces, Long idQuestion) {
        String cle = codeAcces + "/" + idQuestion;
        if (envoisPrevus.add(cle)) {
            planificateur.schedule(() -> {
                envoisPrevus.remove(cle);
                envoyerRepartition(codeAcces, idQuestion);
            }, DELAI_REGROUPEMENT_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void envoyerRepartition(String codeAcces, Long idQuestion) {
        try {
            messages.convertAndSend("/topic/sessions/" + codeAcces + "/repartition",
                    repartition(codeAcces, idQuestion));
        } catch (RuntimeException e) {
            // La réponse est déjà enregistrée : un échec de diffusion ne doit pas la remettre en cause.
            journal.atWarn().addKeyValue("idQuestion", idQuestion).addKeyValue("exception",
                    e.getClass().getSimpleName()).log("repartition_non_diffusee");
        }
    }

    /** Réponses de cette séance seulement : une question peut être jouée dans plusieurs séances. */
    RepartitionMessage repartition(String codeAcces, Long idQuestion) {
        long total = jdbc.sql("""
                        SELECT count(*) FROM reponse r
                          JOIN participant p ON p.id_participant = r.id_participant
                          JOIN session_quiz s ON s.id_session = p.id_session
                         WHERE s.code_acces = :codeAcces AND r.id_question = :idQuestion
                        """)
                .param("codeAcces", codeAcces).param("idQuestion", idQuestion)
                .query(Long.class).single();
        var propositions = jdbc.sql("""
                        SELECT c.id_proposition, count(*) AS nombre FROM reponse_choix c
                          JOIN reponse r ON r.id_reponse = c.id_reponse
                          JOIN participant p ON p.id_participant = r.id_participant
                          JOIN session_quiz s ON s.id_session = p.id_session
                         WHERE s.code_acces = :codeAcces AND r.id_question = :idQuestion
                         GROUP BY c.id_proposition ORDER BY c.id_proposition
                        """)
                .param("codeAcces", codeAcces).param("idQuestion", idQuestion)
                .query((l, i) -> new ParProposition(l.getLong("id_proposition"), l.getLong("nombre")))
                .list();
        return new RepartitionMessage(idQuestion, total, propositions);
    }

    @PreDestroy
    void arreter() {
        planificateur.shutdownNow();
    }
}
