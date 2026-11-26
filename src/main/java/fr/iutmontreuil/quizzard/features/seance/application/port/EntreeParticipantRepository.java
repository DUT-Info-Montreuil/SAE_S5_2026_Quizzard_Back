package fr.iutmontreuil.quizzard.features.seance.application.port;

import java.util.Set;

import fr.iutmontreuil.quizzard.features.seance.domain.ParticipantEntrant;
import fr.iutmontreuil.quizzard.features.seance.domain.Pseudonyme;

/** Port de persistance des participants, implémenté en infrastructure. */
public interface EntreeParticipantRepository {

    /** Vrai si un participant de la séance porte déjà ce pseudonyme (comparaison à l'identique). */
    boolean existePseudonyme(Long idSession, Pseudonyme pseudonyme);

    /** Les pseudonymes déjà utilisés dans la séance (300 au plus, hypothèse du cahier des charges). */
    Set<String> pseudonymesDeLaSession(Long idSession);

    /**
     * Enregistre le participant et force l'écriture, pour qu'une erreur de base soit détectée avant la fin
     * du cas d'usage.
     *
     * @return le participant avec son identifiant
     * @throws fr.iutmontreuil.quizzard.features.seance.domain.PseudonymeDejaPrisException si la contrainte
     *         d'unicité (séance, pseudonyme) est violée, par exemple quand deux étudiants envoient le même
     *         pseudonyme au même instant
     */
    ParticipantEntrant enregistrer(ParticipantEntrant participant);

    /** Nombre de participants de la séance. */
    int compter(Long idSession);
}
