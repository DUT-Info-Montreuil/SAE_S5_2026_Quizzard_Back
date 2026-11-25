package fr.iutmontreuil.quizzard.features.seance.infrastructure.ldap;

import java.time.Duration;
import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.AuthenticationNotSupportedException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.features.seance.application.port.AnnuaireLdap;

/**
 * Disponibilité de l'annuaire de l'IUT : une lecture anonyme de la racine, en liaison chiffrée ({@code ldaps://}),
 * avec un délai court. Aucune écriture, aucun identifiant envoyé.
 * <p>
 * L'adresse vient de {@code quizzard.ldap.url} (variable {@code LDAP_URL}). Sans adresse, ou avec une adresse non
 * chiffrée, l'annuaire est considéré comme indisponible : lancer un quiz noté demande alors l'autorisation de
 * l'enseignant.
 */
@Component
class LdapAnnuaire implements AnnuaireLdap {

    private static final Duration DELAI = Duration.ofSeconds(2);
    private static final Logger journal = LoggerFactory.getLogger("metier");

    private final String url;

    LdapAnnuaire(@Value("${quizzard.ldap.url:${LDAP_URL:}}") String url) {
        this.url = url.strip();
    }

    @Override
    public boolean estDisponible() {
        if (url.isEmpty()) {
            return false;
        }
        if (!url.regionMatches(true, 0, "ldaps://", 0, "ldaps://".length())) {
            journal.warn("annuaire_ignore_liaison_non_chiffree");
            return false;
        }
        Hashtable<String, String> reglages = new Hashtable<>();
        reglages.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        reglages.put(Context.PROVIDER_URL, url);
        reglages.put(Context.SECURITY_AUTHENTICATION, "none");
        reglages.put("com.sun.jndi.ldap.connect.timeout", String.valueOf(DELAI.toMillis()));
        reglages.put("com.sun.jndi.ldap.read.timeout", String.valueOf(DELAI.toMillis()));
        try {
            DirContext annuaire = new InitialDirContext(reglages);
            try {
                annuaire.getAttributes("", new String[] {"namingContexts"});
            } finally {
                annuaire.close();
            }
            return true;
        } catch (AuthenticationException | AuthenticationNotSupportedException | NoPermissionException e) {
            // L'annuaire a répondu, même s'il refuse la lecture anonyme.
            return true;
        } catch (NamingException e) {
            journal.atWarn().addKeyValue("exception", e.getClass().getSimpleName()).log("annuaire_indisponible");
            return false;
        }
    }
}
