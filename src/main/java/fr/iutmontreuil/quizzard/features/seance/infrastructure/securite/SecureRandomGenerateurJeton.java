package fr.iutmontreuil.quizzard.features.seance.infrastructure.securite;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.features.seance.application.port.GenerateurJeton;

/**
 * Jetons de participant : 32 octets tirés par {@link SecureRandom}, encodés en Base64url sans bourrage
 * (43 caractères). Seule leur empreinte ({@code JetonParticipant.empreinte}) est stockée.
 */
@Component
class SecureRandomGenerateurJeton implements GenerateurJeton {

    private static final int OCTETS = 32;

    private final SecureRandom aleatoire = new SecureRandom();

    @Override
    public String generer() {
        byte[] octets = new byte[OCTETS];
        aleatoire.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }
}
