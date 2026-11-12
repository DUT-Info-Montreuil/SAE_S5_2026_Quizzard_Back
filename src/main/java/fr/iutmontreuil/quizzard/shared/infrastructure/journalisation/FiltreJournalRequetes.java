package fr.iutmontreuil.quizzard.shared.infrastructure.journalisation;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Middleware de journalisation des requêtes (cours « Journaliser son API »), le premier de la chaîne,
 * avant Spring Security : il voit aussi les requêtes refusées (401, 403) et les routes inexistantes (404).
 * <ul>
 *   <li>À l'arrivée : crée l'identifiant de requête (MDC + en-tête {@code X-Request-Id}) et démarre le chronomètre.</li>
 *   <li>Au départ de la réponse : écrit l'unique ligne de la requête, {@code request_handled}, dont le niveau
 *       suit le statut (INFO 2xx/3xx, WARN 4xx, ERROR 5xx), avec l'exception rangée par le gestionnaire d'erreurs.</li>
 * </ul>
 * L'identifiant est toujours créé ici : un {@code X-Request-Id} envoyé par un client externe n'est pas repris.
 */
public class FiltreJournalRequetes extends OncePerRequestFilter {

    private static final Logger journal = LoggerFactory.getLogger("http");

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain chaine)
            throws ServletException, IOException {
        long debut = System.nanoTime();
        String requestId = UUID.randomUUID().toString();
        MDC.put(ChampsJournal.MDC_REQUEST_ID, requestId);
        reponse.setHeader(ChampsJournal.EN_TETE_REQUEST_ID, requestId);
        try {
            chaine.doFilter(requete, reponse);
            ecrire(requete, reponse.getStatus(), debut);
        } catch (IOException | ServletException | RuntimeException | Error e) {
            // Exception qui a échappé au gestionnaire d'erreurs (levée dans un filtre, par exemple).
            requete.setAttribute(ChampsJournal.ATTRIBUT_EXCEPTION, e.getClass().getSimpleName());
            journal.atError().addKeyValue("exception", e.getClass().getSimpleName())
                    .addKeyValue("stack", ChampsJournal.pile(e))
                    .log("unhandled_exception");
            ecrire(requete, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, debut);
            throw e;
        } finally {
            MDC.remove(ChampsJournal.MDC_REQUEST_ID);
        }
    }

    private static void ecrire(HttpServletRequest requete, int statut, long debut) {
        LoggingEventBuilder ligne = statut >= 500 ? journal.atError()
                : statut >= 400 ? journal.atWarn()
                : journal.atInfo();
        ligne = ligne.addKeyValue("method", requete.getMethod())
                .addKeyValue("path", requete.getRequestURI());
        // Motif de route (/api/cours/{id}) pour regrouper les logs ; absent si aucune route n'a répondu.
        Object route = requete.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (route != null) {
            ligne = ligne.addKeyValue("route", route);
        }
        if (requete.getQueryString() != null) {
            ligne = ligne.addKeyValue("query", requete.getQueryString());
        }
        ligne = ligne.addKeyValue("status", statut)
                .addKeyValue("duration_ms", (System.nanoTime() - debut) / 1_000_000);
        Object exception = requete.getAttribute(ChampsJournal.ATTRIBUT_EXCEPTION);
        if (exception != null) {
            ligne = ligne.addKeyValue("exception", exception);
        }
        Object erreur = requete.getAttribute(ChampsJournal.ATTRIBUT_ERREUR);
        if (erreur != null) {
            ligne = ligne.addKeyValue("error", erreur);
        }
        ligne.log("request_handled");
    }
}
