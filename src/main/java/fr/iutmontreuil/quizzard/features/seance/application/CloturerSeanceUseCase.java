package fr.iutmontreuil.quizzard.features.seance.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.seance.application.dto.DeroulementSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.Quiz;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;

/**
 * « Clore le quiz » (UC7) : l'enseignant termine la séance, même avant la première question ou la dernière. Le lien
 * et le QR code ne valent plus rien.
 */
@Service
public class CloturerSeanceUseCase {

    private final AccesSeance acces;
    private final FinDeSeance finDeSeance;
    private final VueEnseignant vue;

    CloturerSeanceUseCase(AccesSeance acces, FinDeSeance finDeSeance, VueEnseignant vue) {
        this.acces = acces;
        this.finDeSeance = finDeSeance;
        this.vue = vue;
    }

    /**
     * @throws fr.iutmontreuil.quizzard.features.seance.domain.SeanceIntrouvableException séance inconnue (404)
     * @throws fr.iutmontreuil.quizzard.features.seance.domain.AccesSeanceRefuseException autre enseignant (403)
     * @throws fr.iutmontreuil.quizzard.features.seance.domain.SeanceTermineeException    déjà close (409)
     */
    @Transactional
    public DeroulementSeance executer(Long idSession, Long idEnseignant) {
        SeanceAnimee seance = acces.pourModification(idSession, idEnseignant);
        Quiz quiz = acces.quizDe(seance);
        finDeSeance.terminer(seance, quiz);
        return vue.decrire(seance, quiz);
    }
}
