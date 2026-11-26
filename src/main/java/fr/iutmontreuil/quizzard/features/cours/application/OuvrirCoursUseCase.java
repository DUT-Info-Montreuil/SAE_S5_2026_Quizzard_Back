package fr.iutmontreuil.quizzard.features.cours.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.cours.application.dto.CoursConsulte;
import fr.iutmontreuil.quizzard.features.cours.application.port.ConsultationCoursRepository;
import fr.iutmontreuil.quizzard.features.cours.domain.CoursIntrouvableException;

/**
 * Bouton « Ouvrir » de « Mes cours » (E2) : le cours et sa dernière version, que l'éditeur (E3) affiche. Un cours
 * archivé s'ouvre aussi, signalé par {@code archive} : l'éditeur le montre en lecture seule.
 */
@Service
public class OuvrirCoursUseCase {

    private final ConsultationCoursRepository cours;

    public OuvrirCoursUseCase(ConsultationCoursRepository cours) {
        this.cours = cours;
    }

    /**
     * @throws CoursIntrouvableException si le cours n'existe pas ou appartient à un autre enseignant (404)
     */
    @Transactional(readOnly = true)
    public CoursConsulte executer(Long idCours, Long idEnseignant) {
        return cours.trouverParEnseignant(idCours, idEnseignant).orElseThrow(CoursIntrouvableException::new);
    }
}
