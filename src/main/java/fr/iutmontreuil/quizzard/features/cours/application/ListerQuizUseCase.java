package fr.iutmontreuil.quizzard.features.cours.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.cours.application.port.CoursRepository;
import fr.iutmontreuil.quizzard.features.cours.application.port.QuizRepository;
import fr.iutmontreuil.quizzard.features.cours.domain.CoursIntrouvableException;
import fr.iutmontreuil.quizzard.features.cours.domain.QuizDuCours;
import fr.iutmontreuil.quizzard.shared.application.port.EnseignantConnecte;

/**
 * Quiz du cours pour le dialogue de choix de l'éditeur E3 (UC3) : tous les quiz non archivés, y compris ceux qui
 * n'ont plus de page après un retrait.
 */
@Service
public class ListerQuizUseCase {

    private final EnseignantConnecte enseignant;
    private final CoursRepository cours;
    private final QuizRepository quiz;

    public ListerQuizUseCase(EnseignantConnecte enseignant, CoursRepository cours, QuizRepository quiz) {
        this.enseignant = enseignant;
        this.cours = cours;
        this.quiz = quiz;
    }

    /** @throws CoursIntrouvableException si le cours n'existe pas ou appartient à un autre enseignant */
    @Transactional(readOnly = true)
    public List<QuizDuCours> executer(Long idCours) {
        cours.trouver(idCours, enseignant.idEnseignant()).orElseThrow(CoursIntrouvableException::new);
        return quiz.listerActifs(idCours);
    }
}
