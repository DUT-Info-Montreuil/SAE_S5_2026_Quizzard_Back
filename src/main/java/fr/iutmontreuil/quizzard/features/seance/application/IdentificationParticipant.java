package fr.iutmontreuil.quizzard.features.seance.application;

import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.features.seance.application.port.ParticipantRepository;
import fr.iutmontreuil.quizzard.features.seance.application.port.SessionQuizRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.JetonParticipant;
import fr.iutmontreuil.quizzard.features.seance.domain.Participant;
import fr.iutmontreuil.quizzard.features.seance.domain.ParticipantInvalideException;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionQuiz;

/**
 * Retrouve le participant par son jeton et vérifie qu'il appartient à la séance du code d'accès. Jeton absent,
 * inconnu, remplacé ou d'une autre séance, code d'accès inconnu : même refus (401 JETON_INVALIDE), pour ne rien
 * révéler de l'existence d'une séance ou d'un jeton.
 */
@Component
public class IdentificationParticipant {

    private final SessionQuizRepository sessions;
    private final ParticipantRepository participants;

    public IdentificationParticipant(SessionQuizRepository sessions, ParticipantRepository participants) {
        this.sessions = sessions;
        this.participants = participants;
    }

    /** Séance et participant identifiés. */
    public record ParticipantEnSeance(SessionQuiz session, Participant participant) {
    }

    /** @throws ParticipantInvalideException si le jeton ne désigne pas un participant de cette séance */
    public ParticipantEnSeance identifier(String codeAcces, String jeton) {
        if (jeton == null || jeton.isBlank() || codeAcces == null) {
            throw new ParticipantInvalideException();
        }
        Participant participant = participants.trouverParEmpreinteJeton(JetonParticipant.empreinte(jeton))
                .orElseThrow(ParticipantInvalideException::new);
        SessionQuiz session = sessions.trouverParCodeAcces(codeAcces)
                .filter(s -> participant.appartientA(s.idSession()))
                .orElseThrow(ParticipantInvalideException::new);
        return new ParticipantEnSeance(session, participant);
    }
}
