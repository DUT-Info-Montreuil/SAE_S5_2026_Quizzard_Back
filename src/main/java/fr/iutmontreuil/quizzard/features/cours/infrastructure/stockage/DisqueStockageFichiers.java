package fr.iutmontreuil.quizzard.features.cours.infrastructure.stockage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import fr.iutmontreuil.quizzard.features.cours.application.StockageFichierException;
import fr.iutmontreuil.quizzard.features.cours.application.dto.FichierTemporaire;
import fr.iutmontreuil.quizzard.features.cours.application.port.StockageFichiers;

/**
 * Fichiers sur le disque du serveur, dans {@code quizzard.stockage.dossier} (variable {@code STOCKAGE_DOSSIER}), qui
 * n'est servi par aucune route : un PDF n'est lu que par l'application. Les noms sont tirés au hasard, jamais repris
 * du navigateur. Le fichier reçu passe par le sous-dossier {@code temporaire}, sur le même disque que sa destination.
 */
@Component
class DisqueStockageFichiers implements StockageFichiers {

    static final String DOSSIER_TEMPORAIRE = "temporaire";

    private static final Logger journal = LoggerFactory.getLogger("metier");

    private final Path racine;

    DisqueStockageFichiers(@Value("${quizzard.stockage.dossier}") String dossier) {
        this.racine = Path.of(dossier).toAbsolutePath().normalize();
    }

    @Override
    public FichierTemporaire ecrireTemporaire(InputStream contenu) {
        Path chemin = null;
        try {
            Path dossier = Files.createDirectories(racine.resolve(DOSSIER_TEMPORAIRE));
            chemin = Files.createTempFile(dossier, "import-", ".pdf");
            MessageDigest empreinte = sha256();
            long taille;
            try (OutputStream sortie = new DigestOutputStream(Files.newOutputStream(chemin), empreinte)) {
                taille = contenu.transferTo(sortie);
            }
            return new FichierTemporaire(chemin, taille, HexFormat.of().formatHex(empreinte.digest()));
        } catch (IOException e) {
            if (chemin != null) {
                supprimer(chemin);
            }
            throw new StockageFichierException("Écriture du fichier reçu impossible.", e);
        }
    }

    @Override
    public void deplacer(FichierTemporaire fichier, String cheminStockage) {
        Path cible = dansLeStockage(cheminStockage);
        try {
            Files.createDirectories(cible.getParent());
            try {
                Files.move(fichier.chemin(), cible, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(fichier.chemin(), cible);
            }
        } catch (IOException e) {
            throw new StockageFichierException("Rangement du fichier impossible.", e);
        }
        supprimerSiAnnulation(cible);
    }

    @Override
    public void supprimer(Path chemin) {
        try {
            Files.deleteIfExists(chemin);
        } catch (IOException e) {
            // Nom tiré au hasard : il peut être journalisé. Le fichier restant est un orphelin (UC20).
            journal.atWarn().addKeyValue("fichier", String.valueOf(chemin.getFileName()))
                    .addKeyValue("exception", e.getClass().getSimpleName()).log("fichier_non_supprime");
        }
    }

    /** Un chemin enregistré en base ne sort jamais du dossier de stockage. */
    private Path dansLeStockage(String cheminStockage) {
        Path chemin = racine.resolve(cheminStockage).normalize();
        if (!chemin.startsWith(racine) || chemin.equals(racine)) {
            throw new IllegalArgumentException("chemin de stockage hors du dossier de stockage");
        }
        return chemin;
    }

    /**
     * Une transaction annulée ne laisse pas de fichier. Si l'issue de la validation est inconnue, le fichier est
     * gardé : mieux vaut un orphelin qu'un cours sans son PDF.
     */
    private void supprimerSiAnnulation(Path cible) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int statut) {
                if (statut == STATUS_ROLLED_BACK) {
                    supprimer(cible);
                }
            }
        });
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 est fourni par tout JDK", e);
        }
    }
}
