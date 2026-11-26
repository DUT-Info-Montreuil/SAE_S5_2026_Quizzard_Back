package fr.iutmontreuil.quizzard.features.cours.application;

import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.features.cours.application.port.CoursRepository;
import fr.iutmontreuil.quizzard.features.cours.application.port.PageInsereeRepository;
import fr.iutmontreuil.quizzard.features.cours.application.port.QuizRepository;
import fr.iutmontreuil.quizzard.features.cours.domain.Cours;
import fr.iutmontreuil.quizzard.features.cours.domain.CoursArchiveException;
import fr.iutmontreuil.quizzard.features.cours.domain.PageIntrouvableException;
import fr.iutmontreuil.quizzard.features.cours.domain.PagePlacee;
import fr.iutmontreuil.quizzard.features.cours.domain.QuizEnSeanceException;
import fr.iutmontreuil.quizzard.features.cours.domain.VersionCours;
import fr.iutmontreuil.quizzard.features.cours.domain.VersionPerimeeException;
import fr.iutmontreuil.quizzard.shared.application.port.EnseignantConnecte;

/**
 * Contrôles communs au retrait et au déplacement d'une page insérée, mêmes règles que l'insertion (UC3) : page de
 * l'enseignant connecté (sinon 404, comme une page inexistante), cours verrouillé et non archivé, page de la
 * dernière version, quiz qui n'est pas en séance (ses questions changeraient de position).
 */
@Component
public class AccesPageModifiable {

    private final EnseignantConnecte enseignant;
    private final CoursRepository cours;
    private final QuizRepository quiz;
    private final PageInsereeRepository pages;

    public AccesPageModifiable(EnseignantConnecte enseignant, CoursRepository cours, QuizRepository quiz,
                               PageInsereeRepository pages) {
        this.enseignant = enseignant;
        this.cours = cours;
        this.quiz = quiz;
        this.pages = pages;
    }

    /** Page modifiable et dernière version de son cours. */
    public record PageModifiable(PagePlacee page, VersionCours version) {
    }

    /**
     * À appeler dans la transaction du cas d'usage (le cours reste verrouillé jusqu'à sa fin).
     *
     * @throws PageIntrouvableException si la page n'existe pas ou appartient au cours d'un autre enseignant
     * @throws CoursArchiveException    si le cours est archivé
     * @throws VersionPerimeeException  si la page n'est pas dans la dernière version du cours
     * @throws QuizEnSeanceException    si une séance du quiz de la page est en cours
     */
    public PageModifiable verifier(Long idPage) {
        PagePlacee page = pages.trouverParId(idPage).orElseThrow(PageIntrouvableException::new);
        Cours leCours = cours.trouverPourModification(page.idCours(), enseignant.idEnseignant())
                .orElseThrow(PageIntrouvableException::new);
        VersionCours version = leCours.versionModifiable(page.idVersion());
        if (page.idQuiz() != null && quiz.estEnSeance(page.idQuiz())) {
            throw new QuizEnSeanceException();
        }
        return new PageModifiable(page, version);
    }
}
