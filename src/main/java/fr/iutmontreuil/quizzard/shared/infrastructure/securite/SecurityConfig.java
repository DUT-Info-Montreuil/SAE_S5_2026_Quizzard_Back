package fr.iutmontreuil.quizzard.shared.infrastructure.securite;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import fr.iutmontreuil.quizzard.shared.infrastructure.journalisation.ChampsJournal;

/**
 * Chaîne Spring Security du socle : refus par défaut, seules les routes listées ici sont publiques.
 * Ordre des middlewares : journalisation (hors chaîne, en premier), puis cette chaîne.
 * <p>
 * La connexion (comptes locaux puis LDAP), la session et la configuration CSRF pour le front React
 * appartiennent à la feature {@code compte}. D'ici là, toute route non publique répond 401.
 * <p>
 * Interrupteur de développement {@code SECURITE_OUVERTE=true} (fichier .env) : ouvre toutes les routes et
 * coupe le CSRF, pour appeler l'API depuis Postman ou le front tant que la connexion n'existe pas.
 * Fermé par défaut. À supprimer quand la feature {@code compte} sera faite.
 */
@Configuration
class SecurityConfig {

    private static final Logger journal = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    SecurityFilterChain chaineSecurite(HttpSecurity http, ReponsesRefusAcces refus,
                                       @Value("${quizzard.securite.ouverte}") boolean ouverte) throws Exception {
        if (ouverte) {
            journal.warn("securite_ouverte : toutes les routes sont accessibles sans connexion (développement uniquement)");
            http.csrf(AbstractHttpConfigurer::disable);
        }
        http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(routes -> {
                    routes.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                            .requestMatchers("/error").permitAll();
                    if (ouverte) {
                        routes.anyRequest().permitAll();
                    } else {
                        routes.anyRequest().authenticated();
                    }
                })
                .exceptionHandling(erreurs -> erreurs
                        .authenticationEntryPoint(refus)
                        .accessDeniedHandler(refus))
                // API REST consommée par le front React : pas de page de connexion ni de fenêtre Basic.
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /** CORS : seul le front React (FRONT_URL) peut appeler l'API depuis un navigateur, cookies compris. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${quizzard.front.url}") String urlFront) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(urlFront));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setExposedHeaders(List.of(ChampsJournal.EN_TETE_REQUEST_ID));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
