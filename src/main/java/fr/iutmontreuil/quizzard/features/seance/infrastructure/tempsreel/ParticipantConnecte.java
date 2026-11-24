package fr.iutmontreuil.quizzard.features.seance.infrastructure.tempsreel;

import java.security.Principal;

/**
 * Étudiant connecté au canal temps réel, identifié par son jeton dans la trame STOMP {@code CONNECT}. Le jeton reste
 * en mémoire du serveur pour la durée de la connexion : les messages de l'étudiant n'ont pas à le répéter, et il ne
 * peut jamais agir pour un autre participant. Le nom ne contient aucune donnée personnelle.
 */
record ParticipantConnecte(Long idParticipant, Long idSession, String codeAcces, String jeton) implements Principal {

    @Override
    public String getName() {
        return "participant-" + idParticipant;
    }

    @Override
    public String toString() {
        // Jamais le jeton dans un log.
        return getName();
    }
}
