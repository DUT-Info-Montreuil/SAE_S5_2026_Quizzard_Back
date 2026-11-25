package fr.iutmontreuil.quizzard.features.cours.infrastructure.stockage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.features.cours.application.port.LectureFichiersCours;

/**
 * PDF lus sur le disque, dans le dossier de stockage de l'import ({@code quizzard.stockage.dossier}, variable
 * {@code STOCKAGE_DOSSIER}). Un chemin enregistré en base ne sort jamais de ce dossier : un chemin absolu ou avec
 * {@code ../} est refusé, comme à l'écriture.
 */
@Component
class DisqueLectureFichiersCours implements LectureFichiersCours {

    private static final Logger journal = LoggerFactory.getLogger("metier");

    private final Path racine;

    DisqueLectureFichiersCours(@Value("${quizzard.stockage.dossier:stockage}") String dossier) {
        this.racine = Path.of(dossier).toAbsolutePath().normalize();
    }

    @Override
    public Optional<Path> fichierLisible(String cheminStockage) {
        if (cheminStockage == null || cheminStockage.isBlank()) {
            return Optional.empty();
        }
        Path chemin = racine.resolve(cheminStockage).normalize();
        if (!chemin.startsWith(racine) || chemin.equals(racine)) {
            journal.atWarn().log("chemin_stockage_hors_dossier");
            return Optional.empty();
        }
        if (!Files.isRegularFile(chemin) || !Files.isReadable(chemin)) {
            // Nom tiré au hasard à l'import : il peut être journalisé.
            journal.atWarn().addKeyValue("fichier", String.valueOf(chemin.getFileName())).log("fichier_cours_absent");
            return Optional.empty();
        }
        return Optional.of(chemin);
    }
}
