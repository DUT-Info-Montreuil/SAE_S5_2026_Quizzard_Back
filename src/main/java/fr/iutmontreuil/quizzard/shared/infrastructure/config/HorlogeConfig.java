package fr.iutmontreuil.quizzard.shared.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Horloge du serveur, seule à faire foi (échéances, temps de réponse). Les cas d'usage la reçoivent par leur
 * constructeur : un test leur passe une horloge fixe.
 */
@Configuration
class HorlogeConfig {

    @Bean
    Clock horloge() {
        return Clock.systemDefaultZone();
    }
}
