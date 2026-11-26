package fr.iutmontreuil.quizzard.features.seance.application;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.seance.application.dto.DeroulementSeance;
import fr.iutmontreuil.quizzard.features.seance.application.dto.EcranAAfficher;
import fr.iutmontreuil.quizzard.features.seance.application.port.DiffuseurSeance;
import fr.iutmontreuil.quizzard.features.seance.application.port.MinuterieSeance;
import fr.iutmontreuil.quizzard.features.seance.application.port.SeanceAnimeeRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.AucuneQuestionOuverteException;
import fr.iutmontreuil.quizzard.features.seance.domain.EcranIntermediaire;
import fr.iutmontreuil.quizzard.features.seance.domain.Quiz;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;

/**
 * Clôture d'une question (UC7) : avant la fin du temps par l'enseignant, ou automatiquement à l'échéance par la
 * minuterie du serveur. Affiche aussi, à leur heure, les écrans intermédiaires de la question close.
 */
@Service
public class CloreQuestionUseCase {

    private final AccesSeance acces;
    private final FinDeQuestion finDeQuestion;
    private final VueEnseignant vue;
    private final SeanceAnimeeRepository seances;
    private final DiffuseurSeance diffuseur;
    private final MinuterieSeance minuterie;
    private final Clock horloge;

    CloreQuestionUseCase(AccesSeance acces, FinDeQuestion finDeQuestion, VueEnseignant vue,
                         SeanceAnimeeRepository seances, DiffuseurSeance diffuseur, MinuterieSeance minuterie,
                         Clock horloge) {
        this.acces = acces;
        this.finDeQuestion = finDeQuestion;
        this.vue = vue;
        this.seances = seances;
        this.diffuseur = diffuseur;
        this.minuterie = minuterie;
        this.horloge = horloge;
    }

    /**
     * Commande de l'enseignant.
     *
     * @throws fr.iutmontreuil.quizzard.features.seance.domain.SeanceIntrouvableException séance inconnue (404)
     * @throws fr.iutmontreuil.quizzard.features.seance.domain.AccesSeanceRefuseException autre enseignant (403)
     * @throws fr.iutmontreuil.quizzard.features.seance.domain.SeanceTermineeException    séance close (409)
     * @throws AucuneQuestionOuverteException                                             rien à clore (409)
     */
    @Transactional
    public DeroulementSeance executer(Long idSession, Long idEnseignant) {
        SeanceAnimee seance = acces.pourModification(idSession, idEnseignant);
        seance.verifierNonTerminee();
        if (!seance.aUneQuestionOuverte()) {
            throw new AucuneQuestionOuverteException();
        }
        Quiz quiz = acces.quizDe(seance);
        finDeQuestion.clore(seance, quiz);
        return vue.decrire(seance, quiz);
    }

    /**
     * Clôture à l'échéance. Sans effet si la question de cette position n'est plus ouverte ; reprogrammée si
     * l'échéance n'est pas encore atteinte sur l'horloge du serveur.
     */
    @Transactional
    public void executerAEcheance(Long idSession, int position) {
        seances.trouverPourModification(idSession)
                .filter(seance -> seance.aUneQuestionOuverte() && seance.positionCourante() == position)
                .ifPresent(seance -> {
                    if (seance.echeanceAtteinte(LocalDateTime.now(horloge))) {
                        finDeQuestion.clore(seance, acces.quizDe(seance));
                    } else {
                        minuterie.programmerCloture(idSession, position, seance.echeanceQuestion());
                    }
                });
    }

    /** Affiche un écran intermédiaire, sauf si la séance est terminée ou passée à une autre question. */
    @Transactional(readOnly = true)
    public void afficherEcran(Long idSession, int position, EcranIntermediaire ecran) {
        seances.trouverParId(idSession)
                .filter(seance -> !seance.estTerminee() && !seance.aUneQuestionOuverte()
                        && seance.positionCourante() == position)
                .ifPresent(seance -> acces.quizDe(seance).question(position)
                        .ifPresent(question -> diffuseur.afficherEcran(seance.codeAcces(),
                                new EcranAAfficher(question.idQuestion(), position, ecran))));
    }
}
