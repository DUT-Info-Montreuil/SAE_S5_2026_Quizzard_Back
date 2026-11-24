package fr.iutmontreuil.quizzard.shared.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Canal temps réel (ARCHITECTURE.md §4, API.md §4) : WebSocket + STOMP sur {@code /ws}, broker simple en mémoire
 * (une seule instance du back). Le serveur pousse sur {@code /topic/...} (diffusion) et {@code /user/queue/...}
 * (message personnel) ; les commandes passent par l'API REST.
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String urlFront;

    WebSocketConfig(@Value("${quizzard.front.url}") String urlFront) {
        this.urlFront = urlFront;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registre) {
        registre.addEndpoint("/ws").setAllowedOrigins(urlFront);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registre) {
        registre.enableSimpleBroker("/topic", "/queue");
        registre.setApplicationDestinationPrefixes("/app");
        registre.setUserDestinationPrefix("/user");
    }
}
