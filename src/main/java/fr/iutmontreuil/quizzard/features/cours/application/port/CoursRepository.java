package fr.iutmontreuil.quizzard.features.cours.application.port;

import java.time.LocalDateTime;
import java.util.Optional;

import fr.iutmontreuil.quizzard.features.cours.domain.Cours;

/** Accès aux cours d'un enseignant. */
public interface CoursRepository {

    /**
     * Cours avec sa dernière version, verrouillé jusqu'à la fin de la transaction (SELECT … FOR UPDATE) : deux
     * insertions simultanées dans le même cours s'exécutent l'une après l'autre.
     *
     * @return vide si le cours n'existe pas ou appartient à un autre enseignant
     */
    Optional<Cours> trouverPourModification(Long idCours, Long idEnseignant);

    /** Met à jour la date de dernière modification du cours (affichée sur E2). */
    void marquerModifie(Long idCours, LocalDateTime maintenant);
}
