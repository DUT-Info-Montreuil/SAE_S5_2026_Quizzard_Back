package fr.iutmontreuil.quizzard.features.seance.infrastructure.tempsreel;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import fr.iutmontreuil.quizzard.features.seance.application.dto.CorrectionQuestion;
import fr.iutmontreuil.quizzard.features.seance.application.dto.EcranAAfficher;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QuestionProjetee;
import fr.iutmontreuil.quizzard.features.seance.application.dto.ResultatPersonnel;
import fr.iutmontreuil.quizzard.features.seance.application.port.DiffuseurSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.EtatSession;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;
import fr.iutmontreuil.quizzard.features.seance.infrastructure.format.FormatSeance;

/**
 * Diffusion STOMP du déroulement d'une séance (API.md §3.4). Le canal d'une séance est l'ensemble des destinations
 * {@code /topic/sessions/{codeAcces}/…} du broker. Les envois attendent la validation de la transaction ; un échec
 * d'envoi ne remet pas en cause ce qui est enregistré : un écran rechargé relit l'état par l'API.
 */
@Component
class StompDiffuseurSeance implements DiffuseurSeance {

    /** Nom de l'étudiant connecté, posé à la connexion STOMP par l'UC9 ({@code ParticipantConnecte}). */
    static final String PREFIXE_PARTICIPANT = "participant-";
    static final String FILE_RESULTAT = "/queue/resultat";

    private static final Logger journal = LoggerFactory.getLogger("metier");

    private final SimpMessagingTemplate messagerie;
    private final AbstractBrokerMessageHandler broker;
    private final FormatSeance format;

    StompDiffuseurSeance(SimpMessagingTemplate messagerie,
                         @Qualifier("simpleBrokerMessageHandler") AbstractBrokerMessageHandler broker,
                         FormatSeance format) {
        this.messagerie = messagerie;
        this.broker = broker;
        this.format = format;
    }

    /** {@code /topic/sessions/{codeAcces}/etat} : même forme que celle lue par le front de l'UC9. */
    record EtatMessage(String etat, int position, Long idQuestion, OffsetDateTime echeance) {
    }

    /** {@code /user/queue/resultat} : résultat d'une question, pour un seul étudiant. */
    record ResultatMessage(Long idQuestion, int points, int scoreTotal, int rang, int nombreParticipants,
                           int evolution, boolean repondu, List<String> bonneReponse) {
    }

    /** {@code /topic/sessions/{codeAcces}/ecran} : écran à projeter maintenant. */
    record EcranMessage(Long idQuestion, int position, String type, int rang, int delaiApresClotureSec) {
    }

    @Override
    public void ouvrirCanal(SeanceAnimee seance) {
        if (!broker.isRunning()) {
            throw new CanalTempsReelException();
        }
        String codeAcces = seance.codeAcces();
        EtatMessage etat = new EtatMessage(seance.etat().name(), seance.positionCourante(), null, null);
        apresValidation(() -> diffuser(codeAcces, "etat", etat));
    }

    @Override
    public void annoncerQuestion(String codeAcces, QuestionProjetee question) {
        EtatMessage etat = new EtatMessage(EtatSession.EN_COURS.name(), question.position(), question.idQuestion(),
                format.date(question.echeance()));
        Object message = format.question(question);
        apresValidation(() -> {
            diffuser(codeAcces, "etat", etat);
            diffuser(codeAcces, "question", message);
        });
    }

    @Override
    public void annoncerCorrection(String codeAcces, CorrectionQuestion correction,
                                   List<ResultatPersonnel> resultats) {
        EtatMessage etat = new EtatMessage(EtatSession.EN_COURS.name(), correction.position(),
                correction.idQuestion(), null);
        Object message = format.correction(correction);
        apresValidation(() -> {
            diffuser(codeAcces, "etat", etat);
            diffuser(codeAcces, "correction", message);
            resultats.forEach(this::envoyerResultat);
        });
    }

    @Override
    public void afficherEcran(String codeAcces, EcranAAfficher ecran) {
        EcranMessage message = new EcranMessage(ecran.idQuestion(), ecran.position(), ecran.ecran().type().name(),
                ecran.ecran().rang(), ecran.ecran().delaiApresClotureSec());
        apresValidation(() -> diffuser(codeAcces, "ecran", message));
    }

    @Override
    public void annoncerFin(String codeAcces, int position) {
        EtatMessage etat = new EtatMessage(EtatSession.CLOTUREE.name(), position, null, null);
        apresValidation(() -> diffuser(codeAcces, "etat", etat));
    }

    private void diffuser(String codeAcces, String canal, Object message) {
        envoyer(canal, () -> messagerie.convertAndSend("/topic/sessions/" + codeAcces + "/" + canal, message));
    }

    private void envoyerResultat(ResultatPersonnel r) {
        ResultatMessage message = new ResultatMessage(r.idQuestion(), r.points(), r.scoreTotal(), r.rang(),
                r.nombreParticipants(), r.evolution(), r.repondu(), r.bonneReponse());
        envoyer("resultat", () -> messagerie.convertAndSendToUser(PREFIXE_PARTICIPANT + r.idParticipant(),
                FILE_RESULTAT, message));
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

    private static void envoyer(String canal, Runnable envoi) {
        try {
            envoi.run();
        } catch (RuntimeException e) {
            // Ni le code d'accès ni le message dans le log.
            journal.atWarn().addKeyValue("canal", canal).addKeyValue("exception", e.getClass().getSimpleName())
                    .log("diffusion_seance_echouee");
        }
    }
}
