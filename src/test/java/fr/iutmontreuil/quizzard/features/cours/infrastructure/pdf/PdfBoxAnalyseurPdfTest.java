package fr.iutmontreuil.quizzard.features.cours.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import fr.iutmontreuil.quizzard.features.cours.PdfDeTest;
import fr.iutmontreuil.quizzard.features.cours.domain.FichierNonPdfException;
import fr.iutmontreuil.quizzard.features.cours.domain.PdfIllisibleException;

class PdfBoxAnalyseurPdfTest {

    private final PdfBoxAnalyseurPdf analyseur = new PdfBoxAnalyseurPdf();

    @TempDir
    Path dossier;

    @Test
    void les_pages_d_un_pdf_lisible_sont_comptees() throws IOException {
        assertThat(analyseur.analyser(fichier(PdfDeTest.pages(1)))).isEqualTo(1);
        assertThat(analyseur.analyser(fichier(PdfDeTest.pages(42)))).isEqualTo(42);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "%PDF", "Bonjour", "<html>%PDF-1.7</html>", " %PDF-1.7"})
    void un_contenu_sans_la_signature_au_debut_n_est_pas_un_pdf(String contenu) throws IOException {
        Path fichier = fichier(contenu.getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> analyseur.analyser(fichier)).isInstanceOf(FichierNonPdfException.class);
    }

    @Test
    void un_pdf_corrompu_est_illisible() throws IOException {
        Path vide = fichier("%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII));
        Path abime = fichier(PdfDeTest.corrompu("n'importe quoi"));
        Path tronque = fichier(Arrays.copyOf(PdfDeTest.pages(5), 60));

        for (Path fichier : new Path[] {vide, abime, tronque}) {
            assertThatThrownBy(() -> analyseur.analyser(fichier)).isInstanceOf(PdfIllisibleException.class);
        }
    }

    @Test
    void un_pdf_sans_page_est_illisible() throws IOException {
        Path fichier = fichier(PdfDeTest.pages(0));

        assertThatThrownBy(() -> analyseur.analyser(fichier)).isInstanceOf(PdfIllisibleException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"secret", ""})
    void un_pdf_chiffre_est_refuse_avec_ou_sans_mot_de_passe_d_ouverture(String motDePasse) throws IOException {
        Path fichier = fichier(PdfDeTest.chiffre(motDePasse));

        assertThatThrownBy(() -> analyseur.analyser(fichier))
                .isInstanceOf(PdfIllisibleException.class)
                .hasMessage("Le PDF est illisible ou protégé par un mot de passe.")
                .hasNoCause();
    }

    private Path fichier(byte[] contenu) throws IOException {
        return Files.write(Files.createTempFile(dossier, "test-", ".pdf"), contenu);
    }
}
