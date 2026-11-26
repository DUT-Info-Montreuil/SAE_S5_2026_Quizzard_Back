package fr.iutmontreuil.quizzard.features.seance.application.port;

/** Port de diffusion temps réel (STOMP) du nombre de connectés vers l'écran de l'enseignant (UC8). */
public interface DiffuseurConnectes {

    /**
     * Pousse le nombre de connectés vers l'écran de l'enseignant (destination
     * {@code /topic/sessions/{codeAcces}/connectes}). L'envoi a lieu une fois la transaction en cours validée,
     * pour ne jamais annoncer un participant refusé. Ne lève aucune exception : un écran rechargé relit le
     * nombre par {@code GET /api/sessions/{idSession}}.
     */
    void signalerConnectes(String codeAcces, int nombre);
}
