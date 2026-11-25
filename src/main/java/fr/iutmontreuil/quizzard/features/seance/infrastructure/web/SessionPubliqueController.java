package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.iutmontreuil.quizzard.features.seance.application.ConsulterSessionPubliqueUseCase;
import fr.iutmontreuil.quizzard.features.seance.application.RejoindreSessionUseCase;
import fr.iutmontreuil.quizzard.features.seance.application.dto.RejoindreSessionDto;
import jakarta.validation.Valid;

/**
 * Routes publiques des étudiants pour rejoindre un quiz (UC8) : aucun compte ni aucune session, le code d'accès est
 * lu dans l'adresse. Contrôleur mince : validation de forme, un appel de cas d'usage, conversion. Les erreurs
 * métier sont traduites en code HTTP par {@code GestionnaireErreurs}.
 */
@RestController
@RequestMapping("/api/public/sessions")
class SessionPubliqueController {

    private final ConsulterSessionPubliqueUseCase consulterSession;
    private final RejoindreSessionUseCase rejoindreSession;

    SessionPubliqueController(ConsulterSessionPubliqueUseCase consulterSession,
                              RejoindreSessionUseCase rejoindreSession) {
        this.consulterSession = consulterSession;
        this.rejoindreSession = rejoindreSession;
    }

    /** Ouverture du lien ou du QR code : ce qu'il faut pour afficher l'écran E7. {@code 404} code inconnu, {@code 409} séance close. */
    @GetMapping("/{codeAcces}")
    ResponseEntity<SessionPubliqueResponse> consulter(@PathVariable String codeAcces) {
        return ResponseEntity.ok(SessionPubliqueMapper.versReponse(consulterSession.executer(codeAcces)));
    }

    /**
     * Entrée dans la séance avec un pseudonyme. {@code 201} avec le jeton du participant ; la réponse n'est jamais
     * mise en cache, car elle contient ce jeton secret.
     */
    @PostMapping("/{codeAcces}/participants")
    ResponseEntity<ParticipantRejointResponse> rejoindre(@PathVariable String codeAcces,
                                                         @Valid @RequestBody RejoindreSessionRequest requete) {
        var participant = rejoindreSession.executer(new RejoindreSessionDto(codeAcces, requete.pseudonyme()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(SessionPubliqueMapper.versReponse(participant));
    }
}
