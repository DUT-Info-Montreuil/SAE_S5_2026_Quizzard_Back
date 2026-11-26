package fr.iutmontreuil.quizzard.shared.infrastructure.web;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionMetier;
import fr.iutmontreuil.quizzard.shared.infrastructure.journalisation.ChampsJournal;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Erreurs des messages STOMP envoyés par un étudiant ({@code SEND /app/...}) : traduites en code et en statut
 * HTTP équivalent, avec la même correspondance que les routes REST ({@link GestionnaireErreurs#statutDe}), puis
 * envoyées au seul étudiant concerné sur {@code /user/queue/erreurs} (dossier de conception, UC9).
 */
@ControllerAdvice
class GestionnaireErreursTempsReel {

    static final String FILE_ERREURS = "/queue/erreurs";
    private static final Logger journal = LoggerFactory.getLogger("temps_reel");

    private final JsonMapper json;

    GestionnaireErreursTempsReel(JsonMapper json) {
        this.json = json;
    }

    /**
     * Erreur envoyée à l'étudiant.
     *
     * @param requestId identifiant fourni par le client dans son message, pour relier l'erreur à son envoi
     */
    record ErreurTempsReel(String requestId, String code, int statut, String error) {
    }

    @MessageExceptionHandler(ExceptionMetier.class)
    @SendToUser(destinations = FILE_ERREURS, broadcast = false)
    ErreurTempsReel exceptionMetier(ExceptionMetier e, Message<?> message) {
        HttpStatus statut = GestionnaireErreurs.statutDe(e);
        journal.atInfo().addKeyValue("status", statut.value()).addKeyValue("code", e.code())
                .addKeyValue("exception", e.getClass().getSimpleName()).log("message_refuse");
        return new ErreurTempsReel(requestId(message), e.code(), statut.value(), e.getMessage());
    }

    @MessageExceptionHandler({MethodArgumentNotValidException.class, MessageConversionException.class})
    @SendToUser(destinations = FILE_ERREURS, broadcast = false)
    ErreurTempsReel messageMalForme(Exception e, Message<?> message) {
        journal.atInfo().addKeyValue("status", 400).addKeyValue("exception", e.getClass().getSimpleName())
                .log("message_refuse");
        return new ErreurTempsReel(requestId(message), "REQUETE_INVALIDE", 400, "La requête est mal formée.");
    }

    /** Erreur imprévue (base de données…) : message générique, la pile d'appels reste dans les logs. */
    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = FILE_ERREURS, broadcast = false)
    ErreurTempsReel erreurInattendue(Exception e, Message<?> message) {
        journal.atError().addKeyValue("exception", e.getClass().getSimpleName())
                .addKeyValue("stack", ChampsJournal.pile(e)).log("unhandled_exception");
        return new ErreurTempsReel(requestId(message), "ERREUR_SERVEUR", 500, GestionnaireErreurs.MESSAGE_500);
    }

    /** {@code requestId} lu dans le corps JSON du message, ou {@code null} s'il est absent ou illisible. */
    private String requestId(Message<?> message) {
        try {
            String corps = message.getPayload() instanceof byte[] octets
                    ? new String(octets, StandardCharsets.UTF_8)
                    : String.valueOf(message.getPayload());
            JsonNode valeur = json.readTree(corps).get("requestId");
            return valeur == null || !valeur.isString() ? null : valeur.asString();
        } catch (RuntimeException illisible) {
            return null;
        }
    }
}
