package fr.iutmontreuil.quizzard.features.seance.infrastructure.tempsreel;

import java.security.Principal;
import java.time.Clock;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import fr.iutmontreuil.quizzard.features.seance.application.RepondreQuestionUseCase;
import fr.iutmontreuil.quizzard.features.seance.application.dto.ReponseEnregistree;
import fr.iutmontreuil.quizzard.features.seance.application.dto.RepondreQuestionDto;
import fr.iutmontreuil.quizzard.features.seance.domain.Lien;
import fr.iutmontreuil.quizzard.features.seance.domain.ParticipantInvalideException;
import jakarta.validation.Valid;

/**
 * Réponse envoyée par STOMP, comme le décrit le dossier de conception (UC9). Le front actuel répond en REST ; les
 * deux chemins appellent le même cas d'usage et donnent les mêmes erreurs (sur {@code /user/queue/erreurs}).
 * Le participant est celui de la connexion, jamais un identifiant lu dans le message.
 */
@Controller
class ReponseStompController {

    private final RepondreQuestionUseCase repondreQuestion;
    private final Clock horloge;

    ReponseStompController(RepondreQuestionUseCase repondreQuestion, Clock horloge) {
        this.repondreQuestion = repondreQuestion;
        this.horloge = horloge;
    }

    @MessageMapping("/sessions/{idSession}/reponses")
    @SendToUser(destinations = "/queue/reponses", broadcast = false)
    ReponseEnregistreeMessage repondre(@DestinationVariable Long idSession,
                                       @Valid @Payload RepondreQuestionMessage message, Principal principal) {
        if (!(principal instanceof ParticipantConnecte participant) || !participant.idSession().equals(idSession)) {
            throw new ParticipantInvalideException();
        }
        var liens = message.liens() == null ? null
                : message.liens().stream().map(l -> l == null ? null : new Lien(l.gauche(), l.droite())).toList();
        ReponseEnregistree r = repondreQuestion.executer(new RepondreQuestionDto(participant.codeAcces(),
                participant.jeton(), message.idQuestion(), message.idPropositions(), liens, message.texteLibre()));
        return new ReponseEnregistreeMessage(message.requestId(), r.idQuestion(),
                r.horodatageServeur().atZone(horloge.getZone()).toOffsetDateTime());
    }
}
