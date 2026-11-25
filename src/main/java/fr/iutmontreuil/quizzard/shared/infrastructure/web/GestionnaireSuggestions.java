package fr.iutmontreuil.quizzard.shared.infrastructure.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflitAvecSuggestion;

/**
 * Complète {@link GestionnaireErreurs} pour les conflits qui proposent une valeur de remplacement
 * ({@link ExceptionConflitAvecSuggestion}) : ajoute le champ {@code suggestion} au corps de l'erreur.
 * <p>
 * Aucune correspondance exception → statut ici : la réponse (statut, message, champs de log) est d'abord construite
 * par le gestionnaire global. Prioritaire sur lui, sinon il traiterait ces exceptions seul.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class GestionnaireSuggestions {

    private final GestionnaireErreurs gestionnaire;

    GestionnaireSuggestions(GestionnaireErreurs gestionnaire) {
        this.gestionnaire = gestionnaire;
    }

    @ExceptionHandler(ExceptionConflitAvecSuggestion.class)
    ResponseEntity<Object> conflitAvecSuggestion(ExceptionConflitAvecSuggestion e, WebRequest requete) {
        ResponseEntity<Object> reponse = gestionnaire.exceptionMetier(e, requete);
        ReponseErreur erreur = (ReponseErreur) reponse.getBody();
        return ResponseEntity.status(reponse.getStatusCode())
                .headers(reponse.getHeaders())
                .body(new ErreurAvecSuggestion(erreur.error(), erreur.requestId(), erreur.code(), e.suggestion()));
    }

    /** Les champs de {@link ReponseErreur} utiles à un conflit, plus la proposition. Les champs vides ne sont pas écrits. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ErreurAvecSuggestion(
            String error,
            @JsonProperty("request_id") String requestId,
            String code,
            String suggestion) {
    }
}
