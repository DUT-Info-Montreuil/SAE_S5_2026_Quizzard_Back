package fr.iutmontreuil.quizzard.features.cours.application;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import fr.iutmontreuil.quizzard.features.cours.application.dto.FichierTemporaire;
import fr.iutmontreuil.quizzard.features.cours.application.port.AnalyseurPdf;
import fr.iutmontreuil.quizzard.features.cours.application.port.ImportCoursRepository;
import fr.iutmontreuil.quizzard.features.cours.application.port.StockageFichiers;
import fr.iutmontreuil.quizzard.features.cours.domain.CoursImporte;
import fr.iutmontreuil.quizzard.features.cours.domain.VersionImportee;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/** Doublures en mémoire des ports de l'UC2, qui notent l'ordre des appels. */
class DoublesImport {

    static final Path TEMPORAIRE = Path.of("temporaire", "import-1.pdf");
    static final String EMPREINTE = "b".repeat(64);

    final List<String> appels = new ArrayList<>();
    final Stockage stockage = new Stockage();
    final Depot depot = new Depot();
    final Journal journal = new Journal();
    RuntimeException refusAnalyse;
    private final AnalyseurPdf analyseur = chemin -> {
        appels.add("analyser");
        if (refusAnalyse != null) {
            throw refusAnalyse;
        }
        return 12;
    };

    ImporterCoursUseCase casDUsage() {
        return new ImporterCoursUseCase(stockage, analyseur, depot, journal,
                Clock.fixed(Instant.parse("2026-11-25T14:00:00.123456Z"), ZoneOffset.UTC));
    }

    final class Stockage implements StockageFichiers {

        String cheminDeRangement;
        final List<Path> supprimes = new ArrayList<>();

        @Override
        public FichierTemporaire ecrireTemporaire(InputStream contenu) {
            appels.add("ecrireTemporaire");
            return new FichierTemporaire(TEMPORAIRE, 2048, EMPREINTE);
        }

        @Override
        public void deplacer(FichierTemporaire fichier, String cheminStockage) {
            appels.add("deplacer");
            cheminDeRangement = cheminStockage;
        }

        @Override
        public void supprimer(Path chemin) {
            appels.add("supprimer");
            supprimes.add(chemin);
        }
    }

    final class Depot implements ImportCoursRepository {

        CoursImporte existant;
        CoursImporte enregistre;
        RuntimeException panne;

        @Override
        public void verrouillerImports(Long idEnseignant) {
            appels.add("verrou");
        }

        @Override
        public Optional<CoursImporte> trouverParEmpreinte(Long idEnseignant, String empreinte) {
            appels.add("chercherDoublon");
            return Optional.ofNullable(existant);
        }

        @Override
        public CoursImporte enregistrer(CoursImporte cours) {
            appels.add("enregistrer");
            if (panne != null) {
                throw panne;
            }
            VersionImportee version = cours.derniereVersion().avecIdentifiant(21L);
            enregistre = new CoursImporte(11L, cours.titre(), cours.idEnseignant(), cours.archive(),
                    cours.dateCreation(), cours.dateModification(), List.of(version));
            return enregistre;
        }
    }

    final class Journal implements JournalEvenements {

        record Ligne(String type, String nom, Map<String, Object> champs) {
        }

        final List<Ligne> lignes = new ArrayList<>();

        @Override
        public void evenement(String nom, Map<String, Object> champs) {
            lignes.add(new Ligne("evenement", nom, champs));
        }

        @Override
        public void regleRefusee(String nom, Map<String, Object> champs) {
            lignes.add(new Ligne("regleRefusee", nom, champs));
        }

        @Override
        public void actionSensible(String nom, Map<String, Object> champs) {
            appels.add("journal");
            lignes.add(new Ligne("actionSensible", nom, champs));
        }
    }
}
