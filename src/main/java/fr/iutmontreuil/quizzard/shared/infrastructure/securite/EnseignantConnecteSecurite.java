package fr.iutmontreuil.quizzard.shared.infrastructure.securite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.shared.application.port.EnseignantConnecte;
import fr.iutmontreuil.quizzard.shared.domain.ExceptionNonAuthentifie;

/**
 * Enseignant lu dans Spring Security : le nom de l'utilisateur connecté est son {@code identifiant_ldap}. La feature
 * {@code compte} (connexion, UC1) remplira ce contexte ; d'ici là, les tests le simulent ({@code @WithMockUser}).
 * <p>
 * Développement seulement : avec {@code SECURITE_OUVERTE=true}, un visiteur non connecté est pris pour
 * l'enseignant {@code quizzard.dev.identifiant-enseignant}, s'il est renseigné. À supprimer avec l'interrupteur.
 */
@Component
class EnseignantConnecteSecurite implements EnseignantConnecte {

    private final JdbcClient jdbc;
    private final boolean securiteOuverte;
    private final String identifiantDeDeveloppement;

    EnseignantConnecteSecurite(JdbcClient jdbc, @Value("${quizzard.securite.ouverte}") boolean securiteOuverte,
                               @Value("${quizzard.dev.identifiant-enseignant:}") String identifiantDeDeveloppement) {
        this.jdbc = jdbc;
        this.securiteOuverte = securiteOuverte;
        this.identifiantDeDeveloppement = identifiantDeDeveloppement;
    }

    static class EnseignantNonConnecteException extends ExceptionNonAuthentifie {
        EnseignantNonConnecteException() {
            super("Vous devez être connecté.");
        }
    }

    @Override
    public Long idEnseignant() {
        return jdbc.sql("SELECT id_enseignant FROM enseignant WHERE identifiant_ldap = :identifiant")
                .param("identifiant", identifiant())
                .query(Long.class)
                .optional()
                .orElseThrow(EnseignantNonConnecteException::new);
    }

    private String identifiant() {
        Authentication connecte = SecurityContextHolder.getContext().getAuthentication();
        if (connecte != null && connecte.isAuthenticated() && !(connecte instanceof AnonymousAuthenticationToken)) {
            return connecte.getName();
        }
        if (securiteOuverte && !identifiantDeDeveloppement.isBlank()) {
            return identifiantDeDeveloppement;
        }
        throw new EnseignantNonConnecteException();
    }
}
