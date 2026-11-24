package fr.iutmontreuil.quizzard.features.cours.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.cours.application.dto.InsererPageQuizDto;
import fr.iutmontreuil.quizzard.features.cours.application.dto.PageQuizInseree;
import fr.iutmontreuil.quizzard.features.cours.application.port.CoursRepository;
import fr.iutmontreuil.quizzard.features.cours.application.port.PageInsereeRepository;
import fr.iutmontreuil.quizzard.features.cours.application.port.QuizRepository;
import fr.iutmontreuil.quizzard.features.cours.domain.Cours;
import fr.iutmontreuil.quizzard.features.cours.domain.CoursIntrouvableException;
import fr.iutmontreuil.quizzard.features.cours.domain.PageQuiz;
import fr.iutmontreuil.quizzard.features.cours.domain.Question;
import fr.iutmontreuil.quizzard.features.cours.domain.Quiz;
import fr.iutmontreuil.quizzard.features.cours.domain.QuizEnSeanceException;
import fr.iutmontreuil.quizzard.features.cours.domain.QuizIntrouvableException;
import fr.iutmontreuil.quizzard.features.cours.domain.VersionCours;
import fr.iutmontreuil.quizzard.shared.application.port.EnseignantConnecte;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/**
 * UC3 « Insérer une page de quiz dans le cours ». Tout ou rien, dans une seule transaction, cours verrouillé : le
 * quiz éventuellement créé, la question vierge, le décalage des rangs et la page. Une erreur annule tout.
 */
@Service
public class InsererPageQuizUseCase {

    private final EnseignantConnecte enseignant;
    private final CoursRepository cours;
    private final QuizRepository quiz;
    private final PageInsereeRepository pages;
    private final JournalEvenements journal;
    private final Clock horloge;

    public InsererPageQuizUseCase(EnseignantConnecte enseignant, CoursRepository cours, QuizRepository quiz,
                                  PageInsereeRepository pages, JournalEvenements journal, Clock horloge) {
        this.enseignant = enseignant;
        this.cours = cours;
        this.quiz = quiz;
        this.pages = pages;
        this.journal = journal;
        this.horloge = horloge;
    }

    @Transactional
    public PageQuizInseree executer(InsererPageQuizDto dto) {
        Cours leCours = cours.trouverPourModification(dto.idCours(), enseignant.idEnseignant())
                .orElseThrow(CoursIntrouvableException::new);
        VersionCours version = leCours.versionModifiable(dto.idVersion());
        version.verifierEmplacement(dto.numeroPage(), dto.ordre(),
                pages.compterAuMemeEndroit(version.idVersion(), dto.numeroPage()));

        boolean quizCree = dto.idQuiz() == null;
        Quiz leQuiz = quizCree
                ? Quiz.nouveau(leCours.idCours(), quiz.compterQuiz(leCours.idCours()) + 1)
                : quiz.trouverActif(dto.idQuiz(), leCours.idCours()).orElseThrow(QuizIntrouvableException::new);
        if (!quizCree && quiz.estEnSeance(leQuiz.idQuiz())) {
            throw new QuizEnSeanceException();
        }
        Question question = leQuiz.ajouterQuestionVierge();
        quiz.enregistrer(leQuiz);

        pages.decalerOrdres(version.idVersion(), dto.numeroPage(), dto.ordre());
        PageQuiz page = pages.enregistrer(PageQuiz.creer(version.idVersion(), dto.numeroPage(), dto.ordre(),
                dto.repereTexte(), question.idQuestion()));
        cours.marquerModifie(leCours.idCours(), LocalDateTime.now(horloge));

        journal.evenement("page_quiz_inseree", Map.of("idCours", leCours.idCours(), "idVersion",
                version.idVersion(), "idPage", page.idPage(), "idQuiz", leQuiz.idQuiz(), "quizCree", quizCree));
        return new PageQuizInseree(page, leQuiz, quizCree, question.idQuestion(),
                pages.listerParVersion(version.idVersion()));
    }
}
