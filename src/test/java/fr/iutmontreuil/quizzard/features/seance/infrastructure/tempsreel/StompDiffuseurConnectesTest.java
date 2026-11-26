package fr.iutmontreuil.quizzard.features.seance.infrastructure.tempsreel;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import fr.iutmontreuil.quizzard.features.seance.infrastructure.tempsreel.StompDiffuseurConnectes.MessageConnectes;

class StompDiffuseurConnectesTest {

    private final SimpMessagingTemplate messagerie = mock(SimpMessagingTemplate.class);
    private final StompDiffuseurConnectes diffuseur = new StompDiffuseurConnectes(messagerie);

    @AfterEach
    void fermerLaTransactionSimulee() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void hors_transaction_le_nombre_est_envoye_tout_de_suite() {
        diffuseur.signalerConnectes("ABC", 3);

        verify(messagerie).convertAndSend("/topic/sessions/ABC/connectes", new MessageConnectes(3));
    }

    @Test
    void dans_une_transaction_l_envoi_attend_le_commit() {
        TransactionSynchronizationManager.initSynchronization();

        diffuseur.signalerConnectes("ABC", 3);
        verifyNoInteractions(messagerie);

        TransactionSynchronizationUtils.triggerAfterCommit();
        verify(messagerie).convertAndSend("/topic/sessions/ABC/connectes", new MessageConnectes(3));
    }

    @Test
    void si_la_transaction_est_annulee_rien_n_est_envoye() {
        TransactionSynchronizationManager.initSynchronization();

        diffuseur.signalerConnectes("ABC", 3);
        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verifyNoInteractions(messagerie);
    }

    @Test
    void un_echec_d_envoi_ne_remonte_pas_au_cas_d_usage() {
        doThrow(new MessagingException("broker indisponible")).when(messagerie)
                .convertAndSend(anyString(), any(Object.class));

        assertThatCode(() -> diffuseur.signalerConnectes("ABC", 3)).doesNotThrowAnyException();
    }
}
