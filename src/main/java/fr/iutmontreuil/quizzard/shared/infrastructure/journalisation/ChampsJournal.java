package fr.iutmontreuil.quizzard.shared.infrastructure.journalisation;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Noms partagés entre le middleware de journalisation, le gestionnaire d'erreurs et les réponses de refus
 * d'accès. Le gestionnaire d'erreurs range l'exception dans la requête ; le middleware la relit pour écrire
 * l'unique ligne de log de la requête (cours « Journaliser son API »).
 * <p>
 * Un nom de champ de log est le même partout : {@code duration_ms}, jamais {@code duree} dans un module.
 */
public final class ChampsJournal {

    /** Clé du MDC : chaque ligne de log de la requête porte cet identifiant. */
    public static final String MDC_REQUEST_ID = "request_id";
    /** En-tête renvoyé au client : un utilisateur qui signale un bug donne la référence de sa requête. */
    public static final String EN_TETE_REQUEST_ID = "X-Request-Id";

    /** Attribut de requête : classe de l'exception, rangée par le gestionnaire d'erreurs. */
    public static final String ATTRIBUT_EXCEPTION = ChampsJournal.class.getName() + ".exception";
    /** Attribut de requête : message d'erreur à journaliser, sans donnée personnelle. */
    public static final String ATTRIBUT_ERREUR = ChampsJournal.class.getName() + ".erreur";

    private ChampsJournal() {
    }

    /** Pile d'appels d'une exception, pour la ligne de log à part d'un 500. */
    public static String pile(Throwable erreur) {
        StringWriter ecrivain = new StringWriter();
        erreur.printStackTrace(new PrintWriter(ecrivain));
        return ecrivain.toString();
    }
}
