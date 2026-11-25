package fr.iutmontreuil.quizzard.features.cours.application.port;

import java.util.List;
import java.util.Optional;

import fr.iutmontreuil.quizzard.features.cours.application.dto.CoursConsulte;

/**
 * Lecture des cours d'un enseignant (écrans E2 et E3). Un cours archivé est retiré de l'espace de l'enseignant : il
 * n'est jamais renvoyé.
 */
public interface ConsultationCoursRepository {

    /** Les cours de l'enseignant, le plus récemment modifié d'abord. */
    List<CoursConsulte> listerParEnseignant(Long idEnseignant);

    /** @return vide si le cours n'existe pas, est archivé ou appartient à un autre enseignant */
    Optional<CoursConsulte> trouverParEnseignant(Long idCours, Long idEnseignant);
}
