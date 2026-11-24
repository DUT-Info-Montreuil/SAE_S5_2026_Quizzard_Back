package fr.iutmontreuil.quizzard.shared.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

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
        registre.setErrorHandler(new ErreursStomp());
    }

    /**
     * Trame ERROR envoyée au client quand un message est refusé (connexion, abonnement) : son en-tête
     * {@code message} porte le code d'origine (par exemple {@code JETON_INVALIDE}), pas l'enveloppe technique de
     * Spring. Ces codes ne contiennent aucune donnée personnelle.
     */
    static class ErreursStomp extends StompSubProtocolErrorHandler {

        @Override
        public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> message, Throwable erreur) {
            Throwable cause = erreur;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            StompHeaderAccessor entetes = StompHeaderAccessor.create(StompCommand.ERROR);
            entetes.setMessage(cause.getMessage());
            entetes.setLeaveMutable(true);
            return MessageBuilder.createMessage(new byte[0], entetes.getMessageHeaders());
        }
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registre) {
        registre.enableSimpleBroker("/topic", "/queue");
        registre.setApplicationDestinationPrefixes("/app");
        registre.setUserDestinationPrefix("/user");
    }
}
