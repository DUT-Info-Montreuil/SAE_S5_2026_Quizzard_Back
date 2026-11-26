package fr.iutmontreuil.quizzard.features.seance.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.features.seance.application.dto.CorrectionQuestion;
import fr.iutmontreuil.quizzard.features.seance.application.dto.EcranAAfficher;
import fr.iutmontreuil.quizzard.features.seance.application.dto.ResultatPersonnel;
import fr.iutmontreuil.quizzard.features.seance.application.port.DiffuseurSeance;
import fr.iutmontreuil.quizzard.features.seance.application.port.MinuterieSeance;
import fr.iutmontreuil.quizzard.features.seance.application.port.SeanceAnimeeRepository;
import fr.iutmontreuil.quizzard.features.seance.application.port.SuiviSeanceRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.ClassementDeSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.EcranIntermediaire;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionDeSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.Quiz;
import fr.iutmontreuil.quizzard.features.seance.domain.ScoreParticipant;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/**
 * Clôture de la question ouverte, qu'elle vienne de l'échéance, de l'enseignant, du passage à la suivante ou de la
 * fin de la séance : les réponses s'arrêtent, la correction et les résultats partent, les écrans intermédiaires et le
 * passage automatique sont programmés. S'exécute dans la transaction du cas d'usage appelant.
 */
@Component
class FinDeQuestion {

    private final SeanceAnimeeRepository seances;
    private final SuiviSeanceRepository suivi;
    private final DiffuseurSeance diffuseur;
    private final MinuterieSeance minuterie;
    private final JournalEvenements journal;
    private final Clock horloge;

    FinDeQuestion(SeanceAnimeeRepository seances, SuiviSeanceRepository suivi, DiffuseurSeance diffuseur,
                  MinuterieSeance minuterie, JournalEvenements journal, Clock horloge) {
        this.seances = seances;
        this.suivi = suivi;
        this.diffuseur = diffuseur;
        this.minuterie = minuterie;
        this.journal = journal;
        this.horloge = horloge;
    }

    /**
     * @throws fr.iutmontreuil.quizzard.features.seance.domain.AucuneQuestionOuverteException sans question ouverte
     */
    void clore(SeanceAnimee seance, Quiz quiz) {
        int position = seance.positionCourante();
        seance.cloreQuestion();
        seances.mettreAJour(seance);
        // Absente seulement si les pages du cours ont changé pendant la séance : la question est close sans correction.
        quiz.question(position).ifPresent(question -> {
            publierCorrection(seance, question, position);
            programmerSuite(seance, quiz, question, position);
        });
        journal.evenement("question_close", Map.of("idSession", seance.idSession(), "position", position));
    }

    private void publierCorrection(SeanceAnimee seance, QuestionDeSeance question, int position) {
        CorrectionQuestion correction = CorrectionQuestion.de(question, position,
                suivi.repartition(seance.idSession(), question.idQuestion()));
        List<ScoreParticipant> scores = suivi.scores(seance.idSession(), question.idQuestion());
        Map<Long, ClassementDeSeance.Rang> rangs = ClassementDeSeance.apresQuestion(scores, position == 1);
        List<String> bonneReponse = CorrectionQuestion.bonneReponseEnTexte(question);
        List<ResultatPersonnel> resultats = scores.stream()
                .map(score -> {
                    ClassementDeSeance.Rang rang = rangs.get(score.idParticipant());
                    return new ResultatPersonnel(score.idParticipant(), question.idQuestion(),
                            score.pointsQuestion(), score.scoreTotal(), rang.rang(), scores.size(), rang.evolution(),
                            score.aRepondu(), bonneReponse);
                })
                .toList();
        diffuseur.annoncerCorrection(seance.codeAcces(), correction, resultats);
    }

    private void programmerSuite(SeanceAnimee seance, Quiz quiz, QuestionDeSeance question, int position) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        for (EcranIntermediaire ecran : question.ecrans()) {
            if (ecran.delaiApresClotureSec() == 0) {
                diffuseur.afficherEcran(seance.codeAcces(),
                        new EcranAAfficher(question.idQuestion(), position, ecran));
            } else {
                minuterie.programmerEcran(seance.idSession(), position, ecran,
                        maintenant.plusSeconds(ecran.delaiApresClotureSec()));
            }
        }
        if (quiz.passageAutomatique()) {
            minuterie.programmerQuestionSuivante(seance.idSession(), position,
                    maintenant.plus(quiz.attenteAvantSuivante(question)));
        }
    }
}
