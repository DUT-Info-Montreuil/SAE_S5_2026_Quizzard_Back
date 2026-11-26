package fr.iutmontreuil.quizzard.features.cours.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.cours.application.dto.FichierTemporaire;
import fr.iutmontreuil.quizzard.features.cours.application.dto.ImporterCoursDto;
import fr.iutmontreuil.quizzard.features.cours.application.port.AnalyseurPdf;
import fr.iutmontreuil.quizzard.features.cours.application.port.ImportCoursRepository;
import fr.iutmontreuil.quizzard.features.cours.application.port.StockageFichiers;
import fr.iutmontreuil.quizzard.features.cours.domain.CoursImporte;
import fr.iutmontreuil.quizzard.features.cours.domain.DoublonCoursException;
import fr.iutmontreuil.quizzard.features.cours.domain.VersionImportee;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/**
 * Cas d'utilisation UC2 « Importer un cours » : contrôle le PDF, refuse un doublon non confirmé, crée le cours et sa
 * version 1, range le fichier, enregistre le tout et trace l'action.
 * <p>
 * L'import est tout ou rien : une seule transaction, dont l'annulation supprime le fichier rangé. Le fichier
 * temporaire est supprimé dans tous les cas. L'action sensible {@code cours_importe} n'est conservée que si l'import
 * réussit ; un doublon refusé n'est tracé que dans les logs.
 */
@Service
public class ImporterCoursUseCase {

    private final StockageFichiers stockage;
    private final AnalyseurPdf analyseur;
    private final ImportCoursRepository cours;
    private final JournalEvenements journal;
    private final Clock horloge;

    public ImporterCoursUseCase(StockageFichiers stockage, AnalyseurPdf analyseur, ImportCoursRepository cours,
                                JournalEvenements journal, Clock horloge) {
        this.stockage = stockage;
        this.analyseur = analyseur;
        this.cours = cours;
        this.journal = journal;
        this.horloge = horloge;
    }

    /**
     * @return le cours créé, avec sa première version et ses identifiants
     * @throws fr.iutmontreuil.quizzard.features.cours.domain.TitreInvalideException titre refusé (422)
     * @throws fr.iutmontreuil.quizzard.features.cours.domain.FichierNonPdfException pas un PDF (415)
     * @throws fr.iutmontreuil.quizzard.features.cours.domain.PdfIllisibleException  PDF illisible (422)
     * @throws DoublonCoursException                                                 doublon non confirmé (409)
     * @throws StockageFichierException                                              disque en panne (500)
     */
    @Transactional
    public CoursImporte executer(ImporterCoursDto dto) {
        // Un titre refusé n'écrit rien sur le disque.
        String titre = CoursImporte.nettoyerTitre(dto.titre());
        FichierTemporaire temporaire = stockage.ecrireTemporaire(dto.contenu());
        try {
            int nbPages = analyseur.analyser(temporaire.chemin());
            refuserDoublon(dto, temporaire.empreinte());

            String cheminStockage = "cours/" + UUID.randomUUID() + ".pdf";
            LocalDateTime maintenant = LocalDateTime.now(horloge).truncatedTo(ChronoUnit.MILLIS);
            VersionImportee version = VersionImportee.premiere(dto.nomFichier(), cheminStockage,
                    temporaire.tailleOctets(), nbPages, temporaire.empreinte(), maintenant);
            CoursImporte nouveau = CoursImporte.creer(titre, dto.idEnseignant(), version, maintenant);

            stockage.deplacer(temporaire, cheminStockage);
            CoursImporte enregistre = cours.enregistrer(nouveau);

            Map<String, Object> champs = new LinkedHashMap<>();
            champs.put("cible", enregistre.idCours());
            champs.put("idVersion", enregistre.derniereVersion().idVersion());
            champs.put("nbPages", nbPages);
            champs.put("tailleOctets", temporaire.tailleOctets());
            journal.actionSensible("cours_importe", champs);
            return enregistre;
        } finally {
            stockage.supprimer(temporaire.chemin());
        }
    }

    private void refuserDoublon(ImporterCoursDto dto, String empreinte) {
        cours.verrouillerImports(dto.idEnseignant());
        Optional<CoursImporte> existant = cours.trouverParEmpreinte(dto.idEnseignant(), empreinte);
        if (existant.isPresent() && !dto.confirmerDoublon()) {
            journal.regleRefusee("import_refuse_doublon", Map.of("idCoursExistant", existant.get().idCours()));
            throw new DoublonCoursException(existant.get().titre());
        }
    }
}
