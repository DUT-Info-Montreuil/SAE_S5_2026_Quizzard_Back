package fr.iutmontreuil.quizzard.shared.infrastructure.securite;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.shared.infrastructure.journalisation.ChampsJournal;
import fr.iutmontreuil.quizzard.shared.infrastructure.web.ReponseErreur;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Réponses 401 et 403 décidées par la chaîne Spring Security, avant tout contrôleur : le gestionnaire
 * d'erreurs ne les voit pas, elles sont donc écrites ici, au même format ({@link ReponseErreur}).
 */
@Component
class ReponsesRefusAcces implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final JsonMapper json;

    ReponsesRefusAcces(JsonMapper json) {
        this.json = json;
    }

    /** Pas connecté, ou session absente ou expirée. */
    @Override
    public void commence(HttpServletRequest requete, HttpServletResponse reponse, AuthenticationException e)
            throws IOException {
        ecrire(requete, reponse, e, HttpServletResponse.SC_UNAUTHORIZED, "Vous devez être connecté.");
    }

    /** Connecté sans le droit, ou jeton CSRF absent. */
    @Override
    public void handle(HttpServletRequest requete, HttpServletResponse reponse, AccessDeniedException e)
            throws IOException {
        ecrire(requete, reponse, e, HttpServletResponse.SC_FORBIDDEN, "Action non autorisée.");
    }

    private void ecrire(HttpServletRequest requete, HttpServletResponse reponse, Exception e, int statut,
                        String message) throws IOException {
        requete.setAttribute(ChampsJournal.ATTRIBUT_EXCEPTION, e.getClass().getSimpleName());
        reponse.setStatus(statut);
        reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        reponse.setCharacterEncoding("UTF-8");
        reponse.getWriter().write(json.writeValueAsString(ReponseErreur.de(message)));
    }
}
