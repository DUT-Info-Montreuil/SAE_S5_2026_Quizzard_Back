package fr.iutmontreuil.quizzard.features.cours.infrastructure.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.iutmontreuil.quizzard.features.cours.application.DeplacerPageUseCase;
import fr.iutmontreuil.quizzard.features.cours.application.ListerPagesUseCase;
import fr.iutmontreuil.quizzard.features.cours.application.RetirerPageUseCase;
import jakarta.validation.Valid;

/**
 * Pages insérées dans l'éditeur E3 : lire la liste, retirer une page, la déplacer. Enseignant connecté et
 * propriétaire du cours, lu dans la session ; une page d'un autre enseignant répond comme une page inexistante.
 */
@RestController
class PagesController {

    private final ListerPagesUseCase listerPages;
    private final RetirerPageUseCase retirerPage;
    private final DeplacerPageUseCase deplacerPage;

    PagesController(ListerPagesUseCase listerPages, RetirerPageUseCase retirerPage, DeplacerPageUseCase deplacerPage) {
        this.listerPages = listerPages;
        this.retirerPage = retirerPage;
        this.deplacerPage = deplacerPage;
    }

    @GetMapping("/api/cours/{idCours}/versions/{idVersion}/pages")
    List<PageResponse> lister(@PathVariable Long idCours, @PathVariable Long idVersion) {
        return PageResponse.de(listerPages.executer(idCours, idVersion));
    }

    /** Retire la page ; la question et le quiz sont conservés. 204 sans corps. */
    @DeleteMapping("/api/pages/{idPage}")
    ResponseEntity<Void> retirer(@PathVariable Long idPage) {
        retirerPage.executer(idPage);
        return ResponseEntity.noContent().build();
    }

    /** Déplace la page ; renvoie les pages de la version à jour, dans l'ordre. */
    @PatchMapping("/api/pages/{idPage}")
    List<PageResponse> deplacer(@PathVariable Long idPage, @Valid @RequestBody DeplacerPageRequete requete) {
        return PageResponse.de(deplacerPage.executer(idPage, requete.numeroPage(), requete.ordre()));
    }
}
