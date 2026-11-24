package fr.iutmontreuil.quizzard.features.seance.application.port;

import fr.iutmontreuil.quizzard.features.seance.domain.DejaReponduException;
import fr.iutmontreuil.quizzard.features.seance.domain.Reponse;

/** Enregistrement des réponses des participants. */
public interface ReponseRepository {

    /** Vrai si le participant a déjà une réponse pour cette question. */
    boolean existePour(Long idParticipant, Long idQuestion);

    /**
     * Enregistre la réponse et son contenu (propositions cochées, liens) en forçant l'écriture (flush), pour qu'une
     * erreur de base soit détectée dans le cas d'usage. Renseigne l'identifiant de la réponse.
     *
     * @throws DejaReponduException si deux envois simultanés violent la contrainte d'unicité
     */
    Reponse enregistrer(Reponse reponse);
}
