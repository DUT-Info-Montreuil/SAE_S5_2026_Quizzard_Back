package fr.iutmontreuil.quizzard.features.seance.infrastructure.securite;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.features.seance.application.GenerationAccesException;
import fr.iutmontreuil.quizzard.features.seance.application.port.GenerateurJetonSession;
import fr.iutmontreuil.quizzard.features.seance.domain.JetonSession;

/**
 * Codes d'accès : 16 octets (128 bits) tirés par {@link SecureRandom}, encodés en Base64url sans bourrage
 * (22 caractères, utilisables tels quels dans une adresse et dans une destination STOMP). La contrainte d'unicité de
 * {@code session_quiz.code_acces} reste le dernier rempart contre un doublon.
 */
@Component
class GenerateurJetonSessionSecurise implements GenerateurJetonSession {

    private static final int OCTETS = 16;

    private final SecureRandom aleatoire = new SecureRandom();

    @Override
    public JetonSession generer() {
        try {
            byte[] octets = new byte[OCTETS];
            aleatoire.nextBytes(octets);
            return new JetonSession(Base64.getUrlEncoder().withoutPadding().encodeToString(octets));
        } catch (RuntimeException e) {
            throw new GenerationAccesException("Tirage du code d'accès impossible.", e);
        }
    }
}
