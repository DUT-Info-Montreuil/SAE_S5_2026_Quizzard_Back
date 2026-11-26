package fr.iutmontreuil.quizzard.features.cours.application.port;

import java.util.List;
import java.util.Optional;

import fr.iutmontreuil.quizzard.features.cours.application.dto.CoursConsulte;

/**
 * Lecture des cours d'un enseignant (écrans E2 et E3). Un cours archivé est retiré de la liste « Mes cours », mais son
 * propriétaire peut encore l'ouvrir, en lecture seule.
 */
public interface ConsultationCoursRepository {

    /** Les cours non archivés de l'enseignant, le plus récemment modifié d'abord. */
    List<CoursConsulte> listerParEnseignant(Long idEnseignant);

    /** @return le cours, archivé ou non ; vide s'il n'existe pas ou appartient à un autre enseignant */
    Optional<CoursConsulte> trouverParEnseignant(Long idCours, Long idEnseignant);
}
