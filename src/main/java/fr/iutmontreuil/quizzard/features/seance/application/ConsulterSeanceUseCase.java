package fr.iutmontreuil.quizzard.features.seance.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.seance.application.dto.DeroulementSeance;
import fr.iutmontreuil.quizzard.features.seance.application.dto.SeanceOuverte;
import fr.iutmontreuil.quizzard.features.seance.application.port.SeanceAnimeeRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceIntrouvableException;

/**
 * État d'une séance pour l'écran E5 (UC7) : au chargement, après une coupure réseau, ou pour reprendre la séance
 * restée ouverte.
 */
@Service
public class ConsulterSeanceUseCase {

    private final AccesSeance acces;
    private final VueEnseignant vue;
    private final SeanceAnimeeRepository seances;

    ConsulterSeanceUseCase(AccesSeance acces, VueEnseignant vue, SeanceAnimeeRepository seances) {
        this.acces = acces;
        this.vue = vue;
        this.seances = seances;
    }

    /**
     * @throws SeanceIntrouvableException                                                 séance inconnue (404)
     * @throws fr.iutmontreuil.quizzard.features.seance.domain.AccesSeanceRefuseException autre enseignant (403)
     */
    @Transactional(readOnly = true)
    public DeroulementSeance executer(Long idSession, Long idEnseignant) {
        return decrire(idSession, idEnseignant);
    }

    /** @throws SeanceIntrouvableException si l'enseignant n'a aucune séance non terminée (404) */
    @Transactional(readOnly = true)
    public DeroulementSeance executerPourSeanceOuverte(Long idEnseignant) {
        SeanceOuverte ouverte = seances.trouverOuverteParEnseignant(idEnseignant)
                .orElseThrow(SeanceIntrouvableException::aucuneOuverte);
        return decrire(ouverte.idSession(), idEnseignant);
    }

    private DeroulementSeance decrire(Long idSession, Long idEnseignant) {
        SeanceAnimee seance = acces.pourLecture(idSession, idEnseignant);
        return vue.decrire(seance, acces.quizDe(seance));
    }
}
