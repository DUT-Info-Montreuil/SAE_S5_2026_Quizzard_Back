package fr.iutmontreuil.quizzard.features.seance.application.port;

/** Port de génération des jetons de participant. */
public interface GenerateurJeton {

    /**
     * Tire un jeton secret, remis une seule fois au participant. La base n'en garde que l'empreinte
     * ({@code JetonParticipant.empreinte}).
     */
    String generer();
}
