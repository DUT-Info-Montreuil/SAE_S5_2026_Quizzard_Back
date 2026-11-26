package fr.iutmontreuil.quizzard.features.cours.infrastructure.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.iutmontreuil.quizzard.features.cours.application.InsererPageQuizUseCase;
import fr.iutmontreuil.quizzard.features.cours.application.dto.InsererPageQuizDto;
import fr.iutmontreuil.quizzard.features.cours.application.dto.PageQuizInseree;
import jakarta.validation.Valid;

/**
 * UC3 « Insérer une page de quiz dans le cours » (écran E3). Réservé à l'enseignant connecté, propriétaire du
 * cours ; l'enseignant est lu dans la session, jamais dans la requête.
 */
@RestController
class PageQuizController {

    private final InsererPageQuizUseCase insererPageQuiz;

    PageQuizController(InsererPageQuizUseCase insererPageQuiz) {
        this.insererPageQuiz = insererPageQuiz;
    }

    @PostMapping("/api/cours/{idCours}/pages-quiz")
    ResponseEntity<PageQuizResponse> inserer(@PathVariable Long idCours,
                                             @Valid @RequestBody InsererPageQuizRequete requete) {
        PageQuizInseree resultat = insererPageQuiz.executer(new InsererPageQuizDto(idCours, requete.idVersion(),
                requete.numeroPage(), requete.ordre(), requete.idQuiz(), requete.repereTexte()));
        URI adresse = URI.create("/api/cours/" + idCours + "/pages/" + resultat.page().idPage());
        return ResponseEntity.created(adresse).body(PageQuizResponse.de(resultat));
    }
}
