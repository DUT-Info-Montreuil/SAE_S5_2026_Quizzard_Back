package fr.iutmontreuil.quizzard.features.cours;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;

/** PDF fabriqués pour les tests : aucun cours réel n'entre dans le dépôt. */
public final class PdfDeTest {

    private PdfDeTest() {
    }

    /** PDF valide de {@code nbPages} pages blanches, différent à chaque appel. */
    public static byte[] pages(int nbPages) {
        return ecrire(nbPages, null);
    }

    /** PDF chiffré ; un mot de passe d'ouverture vide laisse le lire sans le saisir. */
    public static byte[] chiffre(String motDePasseOuverture) {
        return ecrire(1, motDePasseOuverture);
    }

    /** Contenu qui commence comme un PDF mais n'en a pas la structure ; il porte le texte donné. */
    public static byte[] corrompu(String texte) {
        return ("%PDF-1.7\n1 0 obj << /Type /Catalog >> " + texte + "\n%%EOF").getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] ecrire(int nbPages, String motDePasseOuverture) {
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < nbPages; i++) {
                document.addPage(new PDPage());
            }
            document.getDocumentInformation().setTitle(UUID.randomUUID().toString());
            if (motDePasseOuverture != null) {
                StandardProtectionPolicy protection = new StandardProtectionPolicy("proprietaire",
                        motDePasseOuverture, new AccessPermission());
                protection.setEncryptionKeyLength(128);
                document.protect(protection);
            }
            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            document.save(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
