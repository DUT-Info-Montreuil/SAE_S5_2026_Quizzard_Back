package fr.iutmontreuil.quizzard.features.seance.application.port;

/** Messages poussés par le serveur sur le canal temps réel (STOMP). */
public interface DiffuseurTempsReel {

    /**
     * Signale une nouvelle réponse à l'écran de l'enseignant (répartition en direct). L'envoi n'a lieu qu'après la
     * validation de la transaction en cours : une réponse annulée n'est jamais annoncée.
     */
    void signalerNouvelleReponse(String codeAcces, Long idQuestion);
}
