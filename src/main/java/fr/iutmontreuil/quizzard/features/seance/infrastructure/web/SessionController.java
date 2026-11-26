package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import java.net.URI;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.iutmontreuil.quizzard.features.seance.application.LancerQuizUseCase;
import fr.iutmontreuil.quizzard.features.seance.application.dto.LancerQuizDto;
import fr.iutmontreuil.quizzard.features.seance.application.dto.SessionLancee;
import fr.iutmontreuil.quizzard.shared.application.port.EnseignantConnecte;

/**
 * UC7 « Lancer un quiz en séance » (écran E3, « Lancer la présentation »). Réservé à l'enseignant connecté,
 * propriétaire du quiz ; l'enseignant est lu dans la session de connexion, jamais dans la requête.
 */
@RestController
class SessionController {

    private final LancerQuizUseCase lancerQuiz;
    private final EnseignantConnecte enseignant;
    private final SessionMapper mapper;

    SessionController(LancerQuizUseCase lancerQuiz, EnseignantConnecte enseignant, SessionMapper mapper) {
        this.lancerQuiz = lancerQuiz;
        this.enseignant = enseignant;
        this.mapper = mapper;
    }

    /** {@code 201} avec la séance, son lien et son QR code ; jamais mise en cache, le code d'accès étant secret. */
    @PostMapping("/api/quiz/{idQuiz}/sessions")
    ResponseEntity<SessionResponse> lancer(@PathVariable Long idQuiz,
                                           @RequestBody(required = false) LancerQuizRequete requete) {
        SessionLancee lancee = lancerQuiz.executer(new LancerQuizDto(idQuiz, enseignant.idEnseignant(),
                LancerQuizRequete.autorise(requete)));
        return ResponseEntity.created(URI.create("/api/sessions/" + lancee.seance().idSession()))
                .cacheControl(CacheControl.noStore())
                .body(mapper.versReponse(lancee));
    }
}
