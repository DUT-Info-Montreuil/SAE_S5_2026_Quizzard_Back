package fr.iutmontreuil.quizzard.features.seance.infrastructure.tempsreel;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/** Branche le contrôle d'accès des étudiants sur les messages entrants du canal temps réel. */
@Configuration
class SeanceWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JetonParticipantInterceptor interceptor;

    SeanceWebSocketConfig(JetonParticipantInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(interceptor);
    }
}
