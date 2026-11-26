package fr.iutmontreuil.quizzard.shared.infrastructure.web;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionAccesRefuse;
import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflit;
import fr.iutmontreuil.quizzard.shared.domain.ExceptionDonneesInvalides;
import fr.iutmontreuil.quizzard.shared.domain.ExceptionIntrouvable;
import fr.iutmontreuil.quizzard.shared.domain.ExceptionMetier;
import fr.iutmontreuil.quizzard.shared.domain.ExceptionNonAuthentifie;
import fr.iutmontreuil.quizzard.shared.domain.ExceptionTypeNonSupporte;
import fr.iutmontreuil.quizzard.shared.infrastructure.journalisation.ChampsJournal;

/**
 * Gestionnaire d'erreurs global : le seul endroit du projet qui associe une exception à un code HTTP.
 * Trois rôles (cours « Journaliser son API ») :
 * <ol>
 *   <li>traduire l'exception en réponse HTTP, avec la même forme ({@link ReponseErreur}) pour toute l'API ;</li>
 *   <li>protéger le client du détail technique : un 500 ne renvoie qu'un message générique ;</li>
 *   <li>ranger l'exception dans la requête pour la ligne de log du middleware.</li>
 * </ol>
 * Il n'écrit pas la ligne de la requête (c'est le middleware {@code FiltreJournalRequetes}) ; pour un 500,
 * il écrit en plus la pile d'appels sur une ligne à part, reliée par {@code request_id}.
 * <p>
 * Une nouvelle exception métier n'a rien à ajouter ici : elle hérite d'une catégorie de {@code shared.domain}.
 */
@RestControllerAdvice
public class GestionnaireErreurs extends ResponseEntityExceptionHandler {

    static final String MESSAGE_500 = "Une erreur est survenue. Réessayez dans un instant.";
    static final String MESSAGE_401 = "Vous devez être connecté.";
    static final String MESSAGE_403 = "Action non autorisée.";

    private static final Logger journal = LoggerFactory.getLogger("http");

    @ExceptionHandler(ExceptionMetier.class)
    ResponseEntity<Object> exceptionMetier(ExceptionMetier e, WebRequest requete) {
        HttpStatus statut = switch (e) {
            case ExceptionIntrouvable _ -> HttpStatus.NOT_FOUND;
            case ExceptionConflit _ -> HttpStatus.CONFLICT;
            case ExceptionAccesRefuse _ -> HttpStatus.FORBIDDEN;
            case ExceptionNonAuthentifie _ -> HttpStatus.UNAUTHORIZED;
            case ExceptionTypeNonSupporte _ -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        ranger(requete, e, e.getMessage());
        if (e instanceof ExceptionDonneesInvalides invalide && invalide.champ() != null) {
            return reponse(statut, ReponseErreur.de(e.getMessage(), Map.of(invalide.champ(), e.getMessage())));
        }
        return reponse(statut, ReponseErreur.de(e.getMessage()));
    }

    /** DTO invalide ({@code @Valid}) : un message par champ, sans recopier les valeurs reçues dans les logs. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                  HttpHeaders entetes, HttpStatusCode statut,
                                                                  WebRequest requete) {
        Map<String, String> champs = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(erreur -> champs.putIfAbsent(erreur.getField(),
                erreur.isBindingFailure() ? "Cette valeur n'est pas valide." : erreur.getDefaultMessage()));
        ranger(requete, e, "champs invalides : " + String.join(", ", champs.keySet()));
        return reponse(HttpStatus.UNPROCESSABLE_ENTITY, ReponseErreur.de("Certaines valeurs sont invalides.", champs));
    }

    /** Refus levé dans un contrôleur ({@code @PreAuthorize}) : 401 pour un visiteur, 403 pour un connecté. */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Object> accesRefuse(AccessDeniedException e, WebRequest requete) {
        ranger(requete, e, null);
        Authentication connecte = SecurityContextHolder.getContext().getAuthentication();
        boolean visiteur = connecte == null || connecte instanceof AnonymousAuthenticationToken;
        return visiteur
                ? reponse(HttpStatus.UNAUTHORIZED, ReponseErreur.de(MESSAGE_401))
                : reponse(HttpStatus.FORBIDDEN, ReponseErreur.de(MESSAGE_403));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Object> nonAuthentifie(AuthenticationException e, WebRequest requete) {
        ranger(requete, e, null);
        return reponse(HttpStatus.UNAUTHORIZED, ReponseErreur.de(MESSAGE_401));
    }

    /**
     * Contrainte PostgreSQL violée (UNIQUE, CHECK…) : le domaine aurait dû refuser avant. Seul le code SQLSTATE
     * est journalisé, car le message de PostgreSQL recopie les valeurs en cause.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Object> contrainteBase(DataIntegrityViolationException e, WebRequest requete) {
        String sqlState = e.getMostSpecificCause() instanceof SQLException sql ? sql.getSQLState() : null;
        ranger(requete, e, "contrainte de base violée, SQLSTATE " + sqlState);
        return reponse(HttpStatus.CONFLICT,
                ReponseErreur.de("Cette opération entre en conflit avec des données existantes."));
    }

    /** Toute exception imprévue : 500 générique, la pile d'appels reste sur le serveur. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> erreurInattendue(Exception e, WebRequest requete) {
        ranger(requete, e, e.getMessage());
        journal.atError().addKeyValue("exception", e.getClass().getSimpleName())
                .addKeyValue("stack", ChampsJournal.pile(e))
                .log("unhandled_exception");
        return reponse(HttpStatus.INTERNAL_SERVER_ERROR, ReponseErreur.de(MESSAGE_500));
    }

    /**
     * Erreurs de Spring MVC (route inconnue, méthode non permise, corps illisible, fichier trop gros…) :
     * le statut choisi par Spring est gardé, la réponse prend la forme commune.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception e, Object corps, HttpHeaders entetes,
                                                             HttpStatusCode statut, WebRequest requete) {
        ranger(requete, e, statut.is5xxServerError() ? e.getMessage() : null);
        if (statut.is5xxServerError()) {
            journal.atError().addKeyValue("exception", e.getClass().getSimpleName())
                    .addKeyValue("stack", ChampsJournal.pile(e))
                    .log("unhandled_exception");
        }
        HttpHeaders copie = new HttpHeaders();
        copie.putAll(entetes);
        copie.setContentType(MediaType.APPLICATION_JSON);
        return ResponseEntity.status(statut).headers(copie).body(ReponseErreur.de(message(statut)));
    }

    private static String message(HttpStatusCode statut) {
        return switch (statut.value()) {
            case 400 -> "La requête est mal formée.";
            case 401 -> MESSAGE_401;
            case 403 -> MESSAGE_403;
            case 404 -> "Ressource introuvable.";
            case 405 -> "Cette méthode n'est pas autorisée sur cette ressource.";
            case 413 -> "Le fichier envoyé est trop volumineux.";
            case 415 -> "Ce type de contenu n'est pas pris en charge.";
            default -> statut.is5xxServerError() ? MESSAGE_500 : "La requête n'a pas abouti.";
        };
    }

    /** Range la classe de l'exception et le message à journaliser pour le middleware. */
    private static void ranger(WebRequest requete, Exception e, String erreur) {
        requete.setAttribute(ChampsJournal.ATTRIBUT_EXCEPTION, e.getClass().getSimpleName(),
                RequestAttributes.SCOPE_REQUEST);
        if (erreur != null) {
            requete.setAttribute(ChampsJournal.ATTRIBUT_ERREUR, erreur, RequestAttributes.SCOPE_REQUEST);
        }
    }

    private static ResponseEntity<Object> reponse(HttpStatus statut, ReponseErreur corps) {
        return ResponseEntity.status(statut).contentType(MediaType.APPLICATION_JSON).body(corps);
    }
}
