package fr.iutmontreuil.quizzard.features.seance.infrastructure.securite;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SecureRandomGenerateurJetonTest {

    private final SecureRandomGenerateurJeton generateur = new SecureRandomGenerateurJeton();

    @Test
    void le_jeton_fait_43_caracteres_base64url_soit_32_octets() {
        String jeton = generateur.generer();

        assertThat(jeton).matches("[A-Za-z0-9_-]{43}");
        assertThat(Base64.getUrlDecoder().decode(jeton)).hasSize(32);
    }

    @Test
    void chaque_jeton_est_different() {
        Set<String> jetons = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            jetons.add(generateur.generer());
        }

        assertThat(jetons).hasSize(1_000);
    }
}
