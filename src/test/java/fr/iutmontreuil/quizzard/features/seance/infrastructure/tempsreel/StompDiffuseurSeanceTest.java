package fr.iutmontreuil.quizzard.features.seance.infrastructure.tempsreel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import fr.iutmontreuil.quizzard.features.seance.application.dto.CorrectionQuestion;
import fr.iutmontreuil.quizzard.features.seance.application.dto.ResultatPersonnel;
import fr.iutmontreuil.quizzard.features.seance.domain.TypeQuestion;
import fr.iutmontreuil.quizzard.features.seance.infrastructure.format.FormatSeance;

class StompDiffuseurSeanceTest {

    private final SimpMessagingTemplate messagerie = mock(SimpMessagingTemplate.class);
    private final AbstractBrokerMessageHandler broker = mock(AbstractBrokerMessageHandler.class);
    private final StompDiffuseurSeance diffuseur = new StompDiffuseurSeance(messagerie, broker,
            new FormatSeance(Clock.systemUTC()));

    @AfterEach
    void finDeTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void un_canal_ne_s_ouvre_pas_si_le_broker_est_arrete() {
        when(broker.isRunning()).thenReturn(false);

        assertThatThrownBy(() -> diffuseur.ouvrirCanal(null)).isInstanceOf(CanalTempsReelException.class);
        verifyNoInteractions(messagerie);
    }

    @Test
    void rien_n_est_envoye_avant_la_validation_de_la_transaction() {
        TransactionSynchronizationManager.initSynchronization();

        diffuseur.annoncerFin("code", 2);
        verifyNoInteractions(messagerie);

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        ArgumentCaptor<Object> message = ArgumentCaptor.forClass(Object.class);
        verify(messagerie).convertAndSend(eq("/topic/sessions/code/etat"), message.capture());
        assertThat(message.getValue())
                .isEqualTo(new StompDiffuseurSeance.EtatMessage("CLOTUREE", 2, null, null));
    }

    @Test
    void chaque_etudiant_recoit_son_resultat_sur_sa_file_personnelle() {
        diffuseur.annoncerCorrection("code", correction(), List.of(
                new ResultatPersonnel(5L, 1L, 900, 900, 1, 2, 0, true, List.of("LDAP")),
                new ResultatPersonnel(6L, 1L, 0, 0, 2, 2, 0, false, List.of("LDAP"))));

        verify(messagerie).convertAndSendToUser(eq("participant-5"), eq("/queue/resultat"),
                eq(new StompDiffuseurSeance.ResultatMessage(1L, 900, 900, 1, 2, 0, true, List.of("LDAP"))));
        verify(messagerie).convertAndSendToUser(eq("participant-6"), eq("/queue/resultat"), any(Object.class));
    }

    @Test
    void un_envoi_en_echec_ne_remet_pas_en_cause_les_suivants() {
        doThrow(new MessagingException("panne")).when(messagerie).convertAndSend(anyString(), any(Object.class));

        diffuseur.annoncerCorrection("code", correction(),
                List.of(new ResultatPersonnel(5L, 1L, 0, 0, 1, 1, 0, false, List.of())));

        verify(messagerie).convertAndSendToUser(eq("participant-5"), eq("/queue/resultat"), any(Object.class));
    }

    private static CorrectionQuestion correction() {
        return new CorrectionQuestion(1L, 1, TypeQuestion.QCM, List.of(10L), List.of(), List.of(), 1, List.of());
    }
}
