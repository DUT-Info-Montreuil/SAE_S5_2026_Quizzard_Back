package fr.iutmontreuil.quizzard.features.cours.infrastructure.stockage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import fr.iutmontreuil.quizzard.features.cours.application.StockageFichierException;
import fr.iutmontreuil.quizzard.features.cours.application.dto.FichierTemporaire;

class DisqueStockageFichiersTest {

    private static final byte[] CONTENU = "%PDF-1.7 contenu".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path racine;

    @AfterEach
    void finDeTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void le_fichier_temporaire_donne_la_taille_reelle_et_l_empreinte() throws Exception {
        FichierTemporaire temporaire = stockage().ecrireTemporaire(new ByteArrayInputStream(CONTENU));

        assertThat(temporaire.chemin()).startsWith(racine.resolve("temporaire")).hasBinaryContent(CONTENU);
        assertThat(temporaire.tailleOctets()).isEqualTo(CONTENU.length);
        assertThat(temporaire.empreinte()).isEqualTo(sha256(CONTENU));
    }

    @Test
    void le_fichier_est_range_a_l_emplacement_demande() {
        DisqueStockageFichiers stockage = stockage();
        FichierTemporaire temporaire = stockage.ecrireTemporaire(new ByteArrayInputStream(CONTENU));

        stockage.deplacer(temporaire, "cours/abc.pdf");

        assertThat(racine.resolve("cours/abc.pdf")).hasBinaryContent(CONTENU);
        assertThat(temporaire.chemin()).doesNotExist();
    }

    @ParameterizedTest
    @CsvSource({"1, false", "0, true", "2, true"})
    void seule_une_transaction_annulee_supprime_le_fichier_range(int statut, boolean garde) {
        DisqueStockageFichiers stockage = stockage();
        FichierTemporaire temporaire = stockage.ecrireTemporaire(new ByteArrayInputStream(CONTENU));
        TransactionSynchronizationManager.initSynchronization();

        stockage.deplacer(temporaire, "cours/abc.pdf");
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(statut));

        assertThat(TransactionSynchronization.STATUS_ROLLED_BACK).isEqualTo(1);
        assertThat(Files.exists(racine.resolve("cours/abc.pdf"))).isEqualTo(garde);
    }

    @ParameterizedTest
    @ValueSource(strings = {"../ailleurs.pdf", "cours/../../ailleurs.pdf", ""})
    void un_chemin_hors_du_dossier_de_stockage_est_refuse(String chemin) {
        DisqueStockageFichiers stockage = stockage();
        FichierTemporaire temporaire = stockage.ecrireTemporaire(new ByteArrayInputStream(CONTENU));

        assertThatThrownBy(() -> stockage.deplacer(temporaire, chemin))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(temporaire.chemin()).exists();
    }

    @Test
    void un_flux_interrompu_ne_laisse_aucun_fichier() throws IOException {
        InputStream interrompu = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("coupure");
            }
        };

        assertThatThrownBy(() -> stockage().ecrireTemporaire(interrompu))
                .isInstanceOf(StockageFichierException.class);

        try (Stream<Path> restes = Files.list(racine.resolve("temporaire"))) {
            assertThat(restes).isEmpty();
        }
    }

    @Test
    void un_dossier_de_stockage_inutilisable_est_une_panne_de_stockage() throws IOException {
        Path pasUnDossier = Files.writeString(racine.resolve("fichier"), "x");

        assertThatThrownBy(() -> new DisqueStockageFichiers(pasUnDossier.toString())
                .ecrireTemporaire(new ByteArrayInputStream(CONTENU)))
                .isInstanceOf(StockageFichierException.class);
    }

    @Test
    void supprimer_un_fichier_absent_est_sans_effet() {
        assertThatCode(() -> stockage().supprimer(racine.resolve("absent.pdf"))).doesNotThrowAnyException();
    }

    private DisqueStockageFichiers stockage() {
        return new DisqueStockageFichiers(racine.toString());
    }

    private static String sha256(byte[] contenu) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu));
    }
}
