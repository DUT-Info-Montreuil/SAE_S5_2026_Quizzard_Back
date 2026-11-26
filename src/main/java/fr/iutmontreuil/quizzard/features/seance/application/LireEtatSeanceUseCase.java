package fr.iutmontreuil.quizzard.features.seance.application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.seance.application.IdentificationParticipant.ParticipantEnSeance;
import fr.iutmontreuil.quizzard.features.seance.application.dto.EtatSeanceEtudiant;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QuestionPourEtudiant;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QuestionPourEtudiant.Choix;
import fr.iutmontreuil.quizzard.features.seance.application.port.QuestionRepository;
import fr.iutmontreuil.quizzard.features.seance.application.port.ReponseRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.Proposition;
import fr.iutmontreuil.quizzard.features.seance.domain.Question;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionPointsARelier;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionQcm;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionReponseLibre;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionQuiz;

/**
 * État de la séance pour un étudiant (écran E8, au chargement et après une reconnexion) : la question ouverte sans
 * ses réponses attendues, si l'étudiant y a déjà répondu, et son score.
 */
@Service
public class LireEtatSeanceUseCase {

    private final IdentificationParticipant identification;
    private final QuestionRepository questions;
    private final ReponseRepository reponses;

    public LireEtatSeanceUseCase(IdentificationParticipant identification, QuestionRepository questions,
                                 ReponseRepository reponses) {
        this.identification = identification;
        this.questions = questions;
        this.reponses = reponses;
    }

    @Transactional(readOnly = true)
    public EtatSeanceEtudiant executer(String codeAcces, String jeton) {
        ParticipantEnSeance acces = identification.identifier(codeAcces, jeton);
        SessionQuiz session = acces.session();
        Long idParticipant = acces.participant().idParticipant();
        int score = acces.participant().scoreTotal();
        if (!session.aUneQuestionOuverte()) {
            return new EtatSeanceEtudiant(null, false, score);
        }
        return questions.trouverParId(session.idQuestionOuverte())
                .map(q -> new EtatSeanceEtudiant(pourEtudiant(q, session),
                        reponses.existePour(idParticipant, q.idQuestion()), score))
                .orElseGet(() -> new EtatSeanceEtudiant(null, false, score));
    }

    /** Vue sans réponses attendues : seuls l'énoncé et les libellés sortent du serveur. */
    public static QuestionPourEtudiant pourEtudiant(Question question, SessionQuiz session) {
        List<Choix> propositions = List.of();
        boolean choixMultiple = false;
        List<Choix> gauche = List.of();
        List<Choix> droite = List.of();
        switch (question) {
            case QuestionQcm qcm -> {
                propositions = qcm.propositions().stream()
                        .sorted(Comparator.comparingInt(Proposition::rang))
                        .map(p -> new Choix(p.idProposition(), p.libelle()))
                        .toList();
                choixMultiple = qcm.choixMultiple();
            }
            case QuestionPointsARelier relier -> {
                gauche = melanger(relier.paires().stream()
                        .map(p -> new Choix(p.idPaire(), p.elementGauche())).toList(), relier.idQuestion());
                droite = melanger(relier.paires().stream()
                        .map(p -> new Choix(p.idPaire(), p.elementDroit())).toList(), ~relier.idQuestion());
            }
            case QuestionReponseLibre _ -> {
                // Ni motif ni réponse attendue : l'énoncé suffit.
            }
        }
        return new QuestionPourEtudiant(question.idQuestion(), session.positionCourante(), question.type(),
                question.enonce(), question.dureeSec(), session.echeanceQuestion(), propositions, choixMultiple,
                gauche, droite);
    }

    /**
     * Mélange stable : même ordre pour tous les étudiants et à chaque rechargement (graine tirée de la question),
     * pour que l'écran ne change pas pendant que l'étudiant relie.
     */
    private static List<Choix> melanger(List<Choix> elements, long graine) {
        List<Choix> copie = new ArrayList<>(elements);
        Collections.shuffle(copie, new Random(graine));
        return List.copyOf(copie);
    }
}
