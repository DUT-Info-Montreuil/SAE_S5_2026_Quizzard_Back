package fr.iutmontreuil.quizzard.features.seance.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.seance.application.dto.LancerQuizDto;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QrCode;
import fr.iutmontreuil.quizzard.features.seance.application.dto.SeanceOuverte;
import fr.iutmontreuil.quizzard.features.seance.application.dto.SessionLancee;
import fr.iutmontreuil.quizzard.features.seance.application.port.AnnuaireLdap;
import fr.iutmontreuil.quizzard.features.seance.application.port.DiffuseurSeance;
import fr.iutmontreuil.quizzard.features.seance.application.port.GenerateurJetonSession;
import fr.iutmontreuil.quizzard.features.seance.application.port.GenerateurQrCode;
import fr.iutmontreuil.quizzard.features.seance.application.port.LiensAcces;
import fr.iutmontreuil.quizzard.features.seance.application.port.QuizDeSeanceRepository;
import fr.iutmontreuil.quizzard.features.seance.application.port.SeanceAnimeeRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.JetonSession;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionDeSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.Quiz;
import fr.iutmontreuil.quizzard.features.seance.domain.QuizIncompletException;
import fr.iutmontreuil.quizzard.features.seance.domain.QuizIntrouvableException;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionDejaOuverteException;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/**
 * Cas d'utilisation UC7 « Lancer un quiz en séance » : contrôle la propriété et l'état du quiz, refuse une seconde
 * séance ouverte, vérifie l'annuaire pour un quiz noté, crée et enregistre la séance, produit le lien et le QR code,
 * ouvre le canal temps réel et trace l'action.
 * <p>
 * Le lancement est tout ou rien : une seule transaction. Le canal n'est annoncé qu'après le commit, et l'action
 * sensible {@code quiz_lance} n'est conservée que si le lancement réussit. Un lancement refusé n'est tracé que dans
 * les logs.
 */
@Service
public class LancerQuizUseCase {

    private final QuizDeSeanceRepository quizzes;
    private final SeanceAnimeeRepository seances;
    private final AnnuaireLdap annuaire;
    private final GenerateurJetonSession generateurJeton;
    private final GenerateurQrCode generateurQrCode;
    private final LiensAcces liens;
    private final DiffuseurSeance diffuseur;
    private final JournalEvenements journal;
    private final Clock horloge;

    public LancerQuizUseCase(QuizDeSeanceRepository quizzes, SeanceAnimeeRepository seances, AnnuaireLdap annuaire,
                             GenerateurJetonSession generateurJeton, GenerateurQrCode generateurQrCode,
                             LiensAcces liens, DiffuseurSeance diffuseur, JournalEvenements journal, Clock horloge) {
        this.quizzes = quizzes;
        this.seances = seances;
        this.annuaire = annuaire;
        this.generateurJeton = generateurJeton;
        this.generateurQrCode = generateurQrCode;
        this.liens = liens;
        this.diffuseur = diffuseur;
        this.journal = journal;
        this.horloge = horloge;
    }

    /**
     * @throws QuizIntrouvableException                                                  quiz inconnu ou archivé (404)
     * @throws fr.iutmontreuil.quizzard.features.seance.domain.AccesQuizRefuseException quiz d'un autre enseignant (403)
     * @throws QuizIncompletException                                                    quiz non prêt (422)
     * @throws SessionDejaOuverteException                                               une séance est ouverte (409)
     * @throws AnnuaireIndisponibleException                                             quiz noté sans annuaire (409)
     * @throws GenerationAccesException                                                  jeton ou QR code (500)
     */
    @Transactional
    public SessionLancee executer(LancerQuizDto dto) {
        Quiz quiz = quizzes.trouverParId(dto.idQuiz()).orElseThrow(QuizIntrouvableException::new);
        quiz.verifierPropriete(dto.idEnseignant());
        verifierPret(quiz);
        verifierAucuneSeanceOuverte(dto.idEnseignant(), quiz);
        boolean identiteNonVerifiee = verifierAnnuaire(quiz, dto.autoriserIdentiteNonVerifiee());

        JetonSession jeton = generateurJeton.generer();
        SeanceAnimee seance = seances.enregistrer(SeanceAnimee.ouvrir(quiz, dto.idEnseignant(), jeton,
                identiteNonVerifiee, LocalDateTime.now(horloge)));
        quizzes.verrouillerQuestions(quiz.questions().stream().map(QuestionDeSeance::idQuestion).toList());

        String lien = liens.lienAcces(jeton);
        QrCode qrCode = generateurQrCode.generer(lien);
        diffuseur.ouvrirCanal(seance);

        Map<String, Object> champs = new LinkedHashMap<>();
        champs.put("cible", seance.idSession());
        champs.put("idQuiz", quiz.idQuiz());
        champs.put("modeNote", seance.modeNote());
        champs.put("nbQuestions", quiz.nbQuestions());
        journal.actionSensible("quiz_lance", champs);
        return new SessionLancee(seance, quiz.titre(), quiz.nbQuestions(), lien, qrCode);
    }

    private void verifierPret(Quiz quiz) {
        try {
            quiz.verifierPretALancer();
        } catch (QuizIncompletException e) {
            List<Integer> pages = e.pages();
            journal.regleRefusee("lancement_refuse_quiz_incomplet",
                    Map.of("idQuiz", quiz.idQuiz(), "pages", pages));
            throw e;
        }
    }

    private void verifierAucuneSeanceOuverte(Long idEnseignant, Quiz quiz) {
        seances.verrouillerLancements(idEnseignant);
        SeanceOuverte ouverte = seances.trouverOuverteParEnseignant(idEnseignant).orElse(null);
        if (ouverte != null) {
            journal.regleRefusee("lancement_refuse_session_ouverte",
                    Map.of("idQuiz", quiz.idQuiz(), "idSession", ouverte.idSession()));
            throw new SessionDejaOuverteException(ouverte.titreCours());
        }
    }

    /** Vrai si la séance doit accepter les entrées en identité non vérifiée : l'annuaire d'un quiz noté est muet. */
    private boolean verifierAnnuaire(Quiz quiz, boolean autorise) {
        if (!quiz.estNote() || annuaire.estDisponible()) {
            return false;
        }
        if (!autorise) {
            journal.regleRefusee("lancement_refuse_annuaire", Map.of("idQuiz", quiz.idQuiz()));
            throw new AnnuaireIndisponibleException();
        }
        return true;
    }
}
