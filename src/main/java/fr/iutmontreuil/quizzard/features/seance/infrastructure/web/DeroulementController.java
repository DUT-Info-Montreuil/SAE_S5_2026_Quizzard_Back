package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.iutmontreuil.quizzard.features.seance.application.CloreQuestionUseCase;
import fr.iutmontreuil.quizzard.features.seance.application.CloturerSeanceUseCase;
import fr.iutmontreuil.quizzard.features.seance.application.ConsulterSeanceUseCase;
import fr.iutmontreuil.quizzard.features.seance.application.DemarrerQuestionUseCase;
import fr.iutmontreuil.quizzard.features.seance.application.dto.DeroulementSeance;
import fr.iutmontreuil.quizzard.shared.application.port.EnseignantConnecte;

/**
 * Déroulement d'une séance par l'enseignant (UC7, écran E5). Chaque commande renvoie l'état à jour de la séance ; les
 * mêmes changements sont poussés sur le canal temps réel.
 */
@RestController
@RequestMapping("/api/sessions")
class DeroulementController {

    private final ConsulterSeanceUseCase consulterSeance;
    private final DemarrerQuestionUseCase demarrerQuestion;
    private final CloreQuestionUseCase cloreQuestion;
    private final CloturerSeanceUseCase cloturerSeance;
    private final EnseignantConnecte enseignant;
    private final SessionMapper mapper;

    DeroulementController(ConsulterSeanceUseCase consulterSeance, DemarrerQuestionUseCase demarrerQuestion,
                          CloreQuestionUseCase cloreQuestion, CloturerSeanceUseCase cloturerSeance,
                          EnseignantConnecte enseignant, SessionMapper mapper) {
        this.consulterSeance = consulterSeance;
        this.demarrerQuestion = demarrerQuestion;
        this.cloreQuestion = cloreQuestion;
        this.cloturerSeance = cloturerSeance;
        this.enseignant = enseignant;
        this.mapper = mapper;
    }

    /** La séance restée ouverte, à reprendre ou à terminer après le 409 {@code SESSION_DEJA_OUVERTE}. */
    @GetMapping("/ouverte")
    ResponseEntity<DeroulementResponse> ouverte() {
        return etat(consulterSeance.executerPourSeanceOuverte(enseignant.idEnseignant()));
    }

    /** État courant : au chargement de E5 et après une coupure réseau. */
    @GetMapping("/{idSession}")
    ResponseEntity<DeroulementResponse> consulter(@PathVariable Long idSession) {
        return etat(consulterSeance.executer(idSession, enseignant.idEnseignant()));
    }

    /** « Démarrer » ou « Suivant » ; après la dernière question, la séance se termine. */
    @PostMapping("/{idSession}/questions/suivante")
    ResponseEntity<DeroulementResponse> questionSuivante(@PathVariable Long idSession) {
        return etat(demarrerQuestion.executer(idSession, enseignant.idEnseignant()));
    }

    /** Clôt la question en cours avant la fin du temps. */
    @PostMapping("/{idSession}/questions/courante/cloture")
    ResponseEntity<DeroulementResponse> cloreQuestionCourante(@PathVariable Long idSession) {
        return etat(cloreQuestion.executer(idSession, enseignant.idEnseignant()));
    }

    /** « Clore le quiz » : termine la séance ; le lien et le QR code ne valent plus rien. */
    @PostMapping("/{idSession}/cloture")
    ResponseEntity<DeroulementResponse> cloturer(@PathVariable Long idSession) {
        return etat(cloturerSeance.executer(idSession, enseignant.idEnseignant()));
    }

    private ResponseEntity<DeroulementResponse> etat(DeroulementSeance etat) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(mapper.versReponse(etat));
    }
}
