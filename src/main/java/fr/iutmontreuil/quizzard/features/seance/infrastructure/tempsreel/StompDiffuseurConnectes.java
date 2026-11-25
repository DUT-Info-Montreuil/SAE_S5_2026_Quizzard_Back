package fr.iutmontreuil.quizzard.features.seance.infrastructure.tempsreel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import fr.iutmontreuil.quizzard.features.seance.application.port.DiffuseurConnectes;

/**
 * Diffusion STOMP du nombre de connectés (UC8). Appelé au milieu d'une transaction, l'envoi attend le commit
 * ({@code afterCommit}) : si la transaction est annulée, rien n'est diffusé, donc aucun participant refusé n'est
 * annoncé. Hors transaction, l'envoi est immédiat.
 * <p>
 * Un échec d'envoi ne remonte pas au cas d'usage : l'entrée est déjà validée, et un écran rechargé relit le nombre
 * de connectés par l'API. Seule la classe de l'exception est journalisée.
 */
@Component
class StompDiffuseurConnectes implements DiffuseurConnectes {

    private static final Logger journal = LoggerFactory.getLogger("metier");

    private final SimpMessagingTemplate messagerie;

    StompDiffuseurConnectes(SimpMessagingTemplate messagerie) {
        this.messagerie = messagerie;
    }

    @Override
    public void signalerConnectes(String codeAcces, int nombre) {
        envoyerApresCommit(() -> messagerie.convertAndSend(
                "/topic/sessions/" + codeAcces + "/connectes", new MessageConnectes(nombre)));
    }

    private void envoyerApresCommit(Runnable envoi) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envoyer(envoi);
                }
            });
        } else {
            envoyer(envoi);
        }
    }

    private static void envoyer(Runnable envoi) {
        try {
            envoi.run();
        } catch (RuntimeException e) {
            journal.atWarn().addKeyValue("exception", e.getClass().getSimpleName()).log("diffusion_connectes_echouee");
        }
    }

    /** Corps du message {@code /topic/sessions/{codeAcces}/connectes}. */
    record MessageConnectes(int nombre) {
    }
}
