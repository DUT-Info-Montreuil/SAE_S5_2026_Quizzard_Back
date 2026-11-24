package fr.iutmontreuil.quizzard.features.seance.application.port;

import java.time.LocalDateTime;
import java.util.Optional;

import fr.iutmontreuil.quizzard.features.seance.domain.Participant;

/** Accès aux participants d'une séance. */
public interface ParticipantRepository {

    /** Participant dont le jeton a cette empreinte SHA-256 (seule valeur stockée en base). */
    Optional<Participant> trouverParEmpreinteJeton(String empreinte);

    /** Ajoute les points au score total et met à jour la dernière activité, en une seule requête UPDATE. */
    void ajouterPoints(Long idParticipant, int points, LocalDateTime maintenant);
}
