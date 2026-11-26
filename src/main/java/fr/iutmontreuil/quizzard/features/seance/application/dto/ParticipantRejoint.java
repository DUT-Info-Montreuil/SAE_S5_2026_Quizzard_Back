package fr.iutmontreuil.quizzard.features.seance.application.dto;

/**
 * Résultat de l'entrée dans la séance : l'identifiant du participant, son jeton <b>en clair</b> (remis une seule
 * fois, jamais relisible ensuite) et le pseudonyme retenu après nettoyage. {@link #toString()} ne montre rien.
 */
public record ParticipantRejoint(Long idParticipant, String jeton, String pseudonyme) {

    @Override
    public String toString() {
        return "ParticipantRejoint[idParticipant=" + idParticipant + "]";
    }
}
