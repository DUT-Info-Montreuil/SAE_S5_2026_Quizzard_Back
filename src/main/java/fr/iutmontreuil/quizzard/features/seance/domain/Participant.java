package fr.iutmontreuil.quizzard.features.seance.domain;

/**
 * Étudiant d'une séance, retrouvé par son jeton. Le pseudonyme n'est pas chargé : le cas d'usage n'en a pas besoin
 * et il ne doit apparaître ni dans les logs ni dans les messages.
 */
public record Participant(Long idParticipant, Long idSession, int scoreTotal) {

    /** Vrai si le participant est entré dans cette séance. */
    public boolean appartientA(Long idSessionVisee) {
        return idSession != null && idSession.equals(idSessionVisee);
    }
}
