package fr.iutmontreuil.quizzard.shared.infrastructure.web;

import java.util.Map;

import org.slf4j.MDC;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.iutmontreuil.quizzard.shared.infrastructure.journalisation.ChampsJournal;

/**
 * La même forme de réponse d'erreur pour toute l'API (cours « Journaliser son API ») :
 * <pre>{"error": "Le cours demandé est introuvable.", "request_id": "…"}</pre>
 * Une erreur de validation ajoute {@code champs} : un message par champ fautif, pour le formulaire du front.
 * Une exception métier qui porte un code ({@code ExceptionMetier.code()}) l'ajoute dans {@code code}.
 * Les champs vides ne sont pas écrits.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReponseErreur(
        String error,
        @JsonProperty("request_id") String requestId,
        Map<String, String> champs,
        String code) {

    public static ReponseErreur de(String message) {
        return new ReponseErreur(message, MDC.get(ChampsJournal.MDC_REQUEST_ID), null, null);
    }

    public static ReponseErreur de(String message, Map<String, String> champs) {
        return new ReponseErreur(message, MDC.get(ChampsJournal.MDC_REQUEST_ID), champs, null);
    }

    /** Erreur métier : son message, ses champs fautifs éventuels et son code éventuel. */
    public static ReponseErreur de(String message, Map<String, String> champs, String code) {
        return new ReponseErreur(message, MDC.get(ChampsJournal.MDC_REQUEST_ID), champs, code);
    }
}
