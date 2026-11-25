package fr.iutmontreuil.quizzard.features.seance.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.seance.application.dto.DeroulementSeance;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QuestionProjetee;
import fr.iutmontreuil.quizzard.features.seance.application.port.DiffuseurSeance;
import fr.iutmontreuil.quizzard.features.seance.application.port.MinuterieSeance;
import fr.iutmontreuil.quizzard.features.seance.application.port.SeanceAnimeeRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionDeSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionDejaOuverteException;
import fr.iutmontreuil.quizzard.features.seance.domain.Quiz;
import fr.iutmontreuil.quizzard.features.seance.domain.QuizIncompletException;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/**
 * « Démarrer » ou « Suivant » (UC7) : ouvre la question suivante, fixe son échéance sur l'horloge du serveur,
 * l'annonce sans ses réponses attendues et programme sa clôture. Après la dernière question, la séance se termine.
 */
@Service
public class DemarrerQuestionUseCase {

    private final AccesSeance acces;
    private final FinDeQuestion finDeQuestion;
    private final FinDeSeance finDeSeance;
    private final VueEnseignant vue;
    private final SeanceAnimeeRepository seances;
    private final DiffuseurSeance diffuseur;
    private final MinuterieSeance minuterie;
    private final JournalEvenements journal;
    private final Clock horloge;

    DemarrerQuestionUseCase(AccesSeance acces, FinDeQuestion finDeQuestion, FinDeSeance finDeSeance,
                            VueEnseignant vue, SeanceAnimeeRepository seances, DiffuseurSeance diffuseur,
                            MinuterieSeance minuterie, JournalEvenements journal, Clock horloge) {
        this.acces = acces;
        this.finDeQuestion = finDeQuestion;
        this.finDeSeance = finDeSeance;
        this.vue = vue;
        this.seances = seances;
        this.diffuseur = diffuseur;
        this.minuterie = minuterie;
        this.journal = journal;
        this.horloge = horloge;
    }

    /**
     * Commande de l'enseignant.
     *
     * @throws fr.iutmontreuil.quizzard.features.seance.domain.SeanceIntrouvableException  séance inconnue (404)
     * @throws fr.iutmontreuil.quizzard.features.seance.domain.AccesSeanceRefuseException  autre enseignant (403)
     * @throws fr.iutmontreuil.quizzard.features.seance.domain.SeanceTermineeException     séance close (409)
     * @throws fr.iutmontreuil.quizzard.features.seance.domain.QuestionDejaOuverteException question ouverte (409)
     * @throws QuizIncompletException la question à ouvrir est devenue incomplète depuis le lancement (422)
     */
    @Transactional
    public DeroulementSeance executer(Long idSession, Long idEnseignant) {
        SeanceAnimee seance = acces.pourModification(idSession, idEnseignant);
        seance.verifierNonTerminee();
        Quiz quiz = acces.quizDe(seance);
        // Échéance passée mais clôture automatique pas encore exécutée : la question est d'abord close.
        if (seance.echeanceAtteinte(LocalDateTime.now(horloge))) {
            finDeQuestion.clore(seance, quiz);
        }
        avancer(seance, quiz);
        return vue.decrire(seance, quiz);
    }

    /**
     * Passage automatique, demandé par la minuterie après la clôture de la question {@code positionClose}. Sans effet
     * si l'enseignant a déjà avancé ou terminé la séance.
     */
    @Transactional
    public void executerAutomatiquement(Long idSession, int positionClose) {
        seances.trouverPourModification(idSession)
                .filter(seance -> !seance.estTerminee() && !seance.aUneQuestionOuverte()
                        && seance.positionCourante() == positionClose)
                .ifPresent(seance -> avancer(seance, acces.quizDe(seance)));
    }

    private void avancer(SeanceAnimee seance, Quiz quiz) {
        if (seance.aUneQuestionOuverte()) {
            throw new QuestionDejaOuverteException();
        }
        Optional<QuestionDeSeance> suivante = quiz.question(seance.positionCourante() + 1);
        if (suivante.isEmpty()) {
            finDeSeance.terminer(seance, quiz);
            return;
        }
        QuestionDeSeance question = suivante.get();
        if (!question.estComplete()) {
            throw QuizIncompletException.questionsIncompletes(List.of(question.pageAffichee()));
        }
        seance.ouvrirQuestionSuivante(question.dureeSec(), LocalDateTime.now(horloge));
        seances.mettreAJour(seance);
        int position = seance.positionCourante();
        diffuseur.annoncerQuestion(seance.codeAcces(),
                QuestionProjetee.de(question, position, seance.echeanceQuestion()));
        minuterie.programmerCloture(seance.idSession(), position, seance.echeanceQuestion());
        journal.evenement("question_ouverte", Map.of("idSession", seance.idSession(), "position", position,
                "idQuestion", question.idQuestion()));
    }
}
