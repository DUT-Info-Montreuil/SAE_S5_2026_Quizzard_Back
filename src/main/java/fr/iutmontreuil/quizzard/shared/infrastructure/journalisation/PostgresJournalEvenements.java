package fr.iutmontreuil.quizzard.shared.infrastructure.journalisation;

import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;
import tools.jackson.databind.json.JsonMapper;

/**
 * Implémentation du port {@link JournalEvenements} : les événements vont dans les logs (logger « metier »,
 * avec le {@code request_id} du MDC) ; les actions sensibles sont aussi écrites dans {@code journal_evenement},
 * avec l'acteur connecté et l'adresse IP de la requête.
 */
@Component
class PostgresJournalEvenements implements JournalEvenements {

    private static final Logger journal = LoggerFactory.getLogger("metier");
    private static final int TAILLE_MAX_NOM = 30;
    private static final int TAILLE_MAX_CIBLE = 100;

    private final JdbcClient jdbc;
    private final JsonMapper json;

    PostgresJournalEvenements(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public void evenement(String nom, Map<String, Object> champs) {
        ecrire(journal.atInfo(), nom, champs);
    }

    @Override
    public void regleRefusee(String nom, Map<String, Object> champs) {
        ecrire(journal.atWarn(), nom, champs);
    }

    @Override
    public void actionSensible(String nom, Map<String, Object> champs) {
        if (nom == null || nom.isBlank() || nom.length() > TAILLE_MAX_NOM) {
            throw new IllegalArgumentException("Nom d'action sensible vide ou de plus de 30 caractères : " + nom);
        }
        ecrire(journal.atInfo(), nom, champs);
        Object cible = champs.get("cible");
        jdbc.sql("""
                INSERT INTO journal_evenement (date_heure, acteur, type_action, cible, adresse_ip, details)
                VALUES (:dateHeure, :acteur, :typeAction, :cible, :adresseIp, :details)
                """)
                .param("dateHeure", LocalDateTime.now())
                .param("acteur", acteurConnecte())
                .param("typeAction", nom)
                .param("cible", cible == null ? null : tronquer(cible.toString(), TAILLE_MAX_CIBLE))
                .param("adresseIp", adresseIp())
                .param("details", champs.isEmpty() ? null : json.writeValueAsString(champs))
                .update();
    }

    private static void ecrire(LoggingEventBuilder ligne, String nom, Map<String, Object> champs) {
        champs.forEach(ligne::addKeyValue);
        ligne.log(nom);
    }

    /** Identifiant de l'utilisateur connecté, ou {@code null} hors requête authentifiée. */
    private static String acteurConnecte() {
        Authentication connecte = SecurityContextHolder.getContext().getAuthentication();
        if (connecte == null || connecte instanceof AnonymousAuthenticationToken || !connecte.isAuthenticated()) {
            return null;
        }
        return tronquer(connecte.getName(), 64);
    }

    /** Adresse IP de la requête en cours, ou {@code null} hors requête HTTP (tâche planifiée…). */
    private static String adresseIp() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributs
                ? attributs.getRequest().getRemoteAddr()
                : null;
    }

    private static String tronquer(String texte, int taille) {
        return texte.length() <= taille ? texte : texte.substring(0, taille);
    }
}
