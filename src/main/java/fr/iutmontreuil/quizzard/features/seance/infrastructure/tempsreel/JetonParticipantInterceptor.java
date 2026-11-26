package fr.iutmontreuil.quizzard.features.seance.infrastructure.tempsreel;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.features.seance.application.IdentificationParticipant;
import fr.iutmontreuil.quizzard.features.seance.application.IdentificationParticipant.ParticipantEnSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.ParticipantInvalideException;

/**
 * Contrôle d'accès du canal temps réel (API.md §4).
 * <ul>
 *   <li>{@code CONNECT} : l'en-tête {@code X-Jeton-Participant} désigne l'étudiant ; un jeton inconnu est refusé
 *   par une trame ERROR dont le message contient {@code JETON_INVALIDE}. Sans jeton, la connexion est réservée à
 *   l'enseignant connecté, ou ouverte à tous en développement ({@code SECURITE_OUVERTE=true}).</li>
 *   <li>{@code SUBSCRIBE} : un étudiant ne s'abonne qu'aux canaux « étudiants » de sa séance et à ses files
 *   personnelles ; la répartition, le classement et les écrans de l'enseignant lui sont refusés.</li>
 * </ul>
 */
@Component
class JetonParticipantInterceptor implements ChannelInterceptor {

    static final String EN_TETE_JETON = "X-Jeton-Participant";
    /** Canaux de diffusion ouverts aux étudiants (les autres sont destinés à l'écran de l'enseignant). */
    private static final Set<String> CANAUX_ETUDIANTS = Set.of("etat", "question", "correction");

    private final IdentificationParticipant identification;
    private final boolean securiteOuverte;

    JetonParticipantInterceptor(IdentificationParticipant identification,
                                @Value("${quizzard.securite.ouverte}") boolean securiteOuverte) {
        this.identification = identification;
        this.securiteOuverte = securiteOuverte;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel canal) {
        StompHeaderAccessor entetes = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (entetes == null || entetes.getCommand() == null) {
            return message;
        }
        if (entetes.getCommand() == StompCommand.CONNECT) {
            connecter(entetes);
        } else if (entetes.getCommand() == StompCommand.SUBSCRIBE) {
            verifierAbonnement(entetes);
        }
        return message;
    }

    private void connecter(StompHeaderAccessor entetes) {
        String jeton = entetes.getFirstNativeHeader(EN_TETE_JETON);
        if (jeton == null) {
            if (!securiteOuverte && entetes.getUser() == null) {
                throw new MessagingException(ParticipantInvalideException.CODE);
            }
            return;
        }
        try {
            ParticipantEnSeance acces = identification.identifier(jeton);
            entetes.setUser(new ParticipantConnecte(acces.participant().idParticipant(),
                    acces.session().idSession(), acces.session().codeAcces(), jeton));
        } catch (ParticipantInvalideException e) {
            throw new MessagingException(ParticipantInvalideException.CODE);
        }
    }

    private static void verifierAbonnement(StompHeaderAccessor entetes) {
        if (!(entetes.getUser() instanceof ParticipantConnecte participant)) {
            return;
        }
        String destination = entetes.getDestination();
        String prefixe = "/topic/sessions/" + participant.codeAcces() + "/";
        boolean autorise = destination != null && (destination.startsWith("/user/queue/")
                || destination.startsWith(prefixe) && CANAUX_ETUDIANTS.contains(destination.substring(prefixe.length())));
        if (!autorise) {
            throw new MessagingException("ABONNEMENT_REFUSE");
        }
    }
}
