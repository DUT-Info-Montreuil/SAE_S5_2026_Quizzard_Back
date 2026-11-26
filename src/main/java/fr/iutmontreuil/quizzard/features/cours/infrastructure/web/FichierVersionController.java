package fr.iutmontreuil.quizzard.features.cours.infrastructure.web;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import fr.iutmontreuil.quizzard.features.cours.application.LireFichierVersionUseCase;

/**
 * PDF d'une version, lu par PDF.js dans l'éditeur E3. Jamais en accès direct : le propriétaire est vérifié à chaque
 * lecture. Les requêtes {@code Range} (lecture par tranches de PDF.js) sont prises en charge par Spring pour une
 * {@link Resource} (réponse 206).
 */
@RestController
class FichierVersionController {

    private final LireFichierVersionUseCase lireFichier;

    FichierVersionController(LireFichierVersionUseCase lireFichier) {
        this.lireFichier = lireFichier;
    }

    @GetMapping("/api/cours/{idCours}/versions/{idVersion}/fichier")
    ResponseEntity<Resource> lire(@PathVariable Long idCours, @PathVariable Long idVersion) {
        Resource pdf = new FileSystemResource(lireFichier.executer(idCours, idVersion));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.empty().cachePrivate())
                .body(pdf);
    }
}
