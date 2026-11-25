package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import fr.iutmontreuil.quizzard.features.seance.application.dto.ParticipantRejoint;
import fr.iutmontreuil.quizzard.features.seance.application.dto.SessionPublique;

/** Convertit les résultats des cas d'usage en réponses JSON pour le navigateur. */
final class SessionPubliqueMapper {

    private SessionPubliqueMapper() {
    }

    static ParticipantRejointResponse versReponse(ParticipantRejoint participant) {
        return new ParticipantRejointResponse(participant.idParticipant(), participant.jeton(),
                participant.pseudonyme());
    }

    static SessionPubliqueResponse versReponse(SessionPublique session) {
        return new SessionPubliqueResponse(session.titreCours(), session.titreQuiz(), session.estNotee(),
                session.etat().name());
    }
}
