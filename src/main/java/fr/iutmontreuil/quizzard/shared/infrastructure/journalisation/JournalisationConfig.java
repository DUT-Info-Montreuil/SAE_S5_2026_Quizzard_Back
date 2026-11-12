package fr.iutmontreuil.quizzard.shared.infrastructure.journalisation;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Branche le middleware de journalisation en tout premier, avant Spring Security : sinon les requêtes
 * rejetées par l'authentification, celles qui intéressent le plus, ne seraient jamais journalisées.
 */
@Configuration
class JournalisationConfig {

    @Bean
    FilterRegistrationBean<FiltreJournalRequetes> filtreJournalRequetes() {
        var enregistrement = new FilterRegistrationBean<>(new FiltreJournalRequetes());
        enregistrement.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return enregistrement;
    }
}
