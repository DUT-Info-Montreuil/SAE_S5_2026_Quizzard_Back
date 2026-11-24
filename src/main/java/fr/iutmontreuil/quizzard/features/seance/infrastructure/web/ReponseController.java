package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.iutmontreuil.quizzard.features.seance.application.LireEtatSeanceUseCase;
import fr.iutmontreuil.quizzard.features.seance.application.RepondreQuestionUseCase;
import fr.iutmontreuil.quizzard.features.seance.application.dto.RepondreQuestionDto;
import fr.iutmontreuil.quizzard.features.seance.domain.Lien;
import jakarta.validation.Valid;

/**
 * Routes de l'étudiant pendant une question (UC9, écran E8). Le participant est désigné par l'en-tête
 * {@code X-Jeton-Participant} ; un jeton absent ou invalide donne 401 JETON_INVALIDE (vérifié par le cas d'usage).
 */
@RestController
@RequestMapping("/api/public/sessions/{codeAcces}")
class ReponseController {

    static final String EN_TETE_JETON = "X-Jeton-Participant";

    private final RepondreQuestionUseCase repondreQuestion;
    private final LireEtatSeanceUseCase lireEtat;
    private final SeanceMapper mapper;

    ReponseController(RepondreQuestionUseCase repondreQuestion, LireEtatSeanceUseCase lireEtat,
                      SeanceMapper mapper) {
        this.repondreQuestion = repondreQuestion;
        this.lireEtat = lireEtat;
        this.mapper = mapper;
    }

    /** Répondre à la question ouverte : 201 avec l'accusé, ou une erreur du tableau de l'UC9. */
    @PostMapping("/reponses")
    ResponseEntity<ReponseEnregistreeResponse> repondre(
            @PathVariable String codeAcces,
            @RequestHeader(name = EN_TETE_JETON, required = false) String jeton,
            @Valid @RequestBody RepondreQuestionRequete requete) {
        var liens = requete.liens() == null ? null
                : requete.liens().stream().map(l -> l == null ? null : new Lien(l.gauche(), l.droite())).toList();
        var dto = new RepondreQuestionDto(codeAcces, jeton, requete.idQuestion(), requete.idPropositions(), liens,
                requete.texteLibre());
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(mapper.versResponse(repondreQuestion.executer(dto)));
    }

    /** État de la séance pour l'étudiant : au chargement de E8 et après chaque reconnexion. */
    @GetMapping("/etat")
    ResponseEntity<EtatSeanceResponse> etat(
            @PathVariable String codeAcces,
            @RequestHeader(name = EN_TETE_JETON, required = false) String jeton) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(mapper.versResponse(lireEtat.executer(codeAcces, jeton)));
    }
}
