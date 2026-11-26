package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

/**
 * Réponse à une entrée réussie. Le {@code jeton} est remis une seule fois : le front le garde pour ce code d'accès
 * et l'envoie ensuite dans l'en-tête {@code X-Jeton-Participant} et dans la trame STOMP {@code CONNECT}.
 * {@link #toString()} ne montre ni le jeton ni le pseudonyme.
 */
public record ParticipantRejointResponse(Long idParticipant, String jeton, String pseudonyme) {

    @Override
    public String toString() {
        return "ParticipantRejointResponse[idParticipant=" + idParticipant + "]";
    }
}
