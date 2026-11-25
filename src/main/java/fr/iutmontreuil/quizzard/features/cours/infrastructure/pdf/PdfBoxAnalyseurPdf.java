package fr.iutmontreuil.quizzard.features.cours.infrastructure.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.features.cours.application.StockageFichierException;
import fr.iutmontreuil.quizzard.features.cours.application.port.AnalyseurPdf;
import fr.iutmontreuil.quizzard.features.cours.domain.FichierNonPdfException;
import fr.iutmontreuil.quizzard.features.cours.domain.PdfIllisibleException;

/**
 * Contrôle d'un PDF avec PDFBox : signature {@code %PDF-} au début du fichier, absence de chiffrement, au moins une
 * page. La lecture passe par un cache sur disque, pour qu'un gros fichier ne remplisse pas la mémoire.
 */
@Component
class PdfBoxAnalyseurPdf implements AnalyseurPdf {

    private static final byte[] SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    @Override
    public int analyser(Path chemin) {
        verifierSignature(chemin);
        int nbPages;
        try (PDDocument document = Loader.loadPDF(chemin.toFile(), IOUtils.createTempFileOnlyStreamCache())) {
            nbPages = document.isEncrypted() ? 0 : document.getNumberOfPages();
        } catch (IOException | RuntimeException | StackOverflowError e) {
            // Mot de passe exigé ou structure abîmée. La cause n'est pas gardée : elle peut citer le fichier.
            throw new PdfIllisibleException();
        }
        if (nbPages < 1) {
            throw new PdfIllisibleException();
        }
        return nbPages;
    }

    private static void verifierSignature(Path chemin) {
        try (InputStream contenu = Files.newInputStream(chemin)) {
            if (!Arrays.equals(contenu.readNBytes(SIGNATURE.length), SIGNATURE)) {
                throw new FichierNonPdfException();
            }
        } catch (IOException e) {
            throw new StockageFichierException("Lecture du fichier reçu impossible.", e);
        }
    }
}
