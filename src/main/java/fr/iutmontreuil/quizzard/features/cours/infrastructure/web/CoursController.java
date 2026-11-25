package fr.iutmontreuil.quizzard.features.cours.infrastructure.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fr.iutmontreuil.quizzard.features.cours.application.ImporterCoursUseCase;
import fr.iutmontreuil.quizzard.features.cours.application.StockageFichierException;
import fr.iutmontreuil.quizzard.features.cours.application.dto.ImporterCoursDto;
import fr.iutmontreuil.quizzard.features.cours.domain.CoursImporte;
import fr.iutmontreuil.quizzard.shared.application.port.EnseignantConnecte;

/**
 * UC2 « Importer un cours » (écran E2). Réservé à l'enseignant connecté, lu dans la session de connexion, jamais dans
 * la requête. Le titre est contrôlé par le domaine, qui répond 422 comme le prévoit le dossier de conception ; une
 * annotation {@code @Valid} donnerait 400 avec le gestionnaire d'erreurs commun.
 */
@RestController
class CoursController {

    private final ImporterCoursUseCase importerCours;
    private final EnseignantConnecte enseignant;

    CoursController(ImporterCoursUseCase importerCours, EnseignantConnecte enseignant) {
        this.importerCours = importerCours;
        this.enseignant = enseignant;
    }

    /** {@code 201} avec le cours créé en version 1. */
    @PostMapping(path = "/api/cours", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<CoursResponse> importer(
            @RequestPart("fichier") MultipartFile fichier,
            @RequestParam(name = "titre", required = false) String titre,
            @RequestParam(name = "confirmerDoublon", defaultValue = "false") boolean confirmerDoublon) {
        Long idEnseignant = enseignant.idEnseignant();
        try (InputStream contenu = fichier.getInputStream()) {
            CoursImporte cours = importerCours.executer(new ImporterCoursDto(titre, fichier.getOriginalFilename(),
                    contenu, idEnseignant, confirmerDoublon));
            return ResponseEntity.created(URI.create("/api/cours/" + cours.idCours()))
                    .body(CoursMapper.versReponse(cours));
        } catch (IOException e) {
            throw new StockageFichierException("Lecture du fichier reçu impossible.", e);
        }
    }
}
