package fr.iutmontreuil.quizzard.features.cours.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;
import fr.iutmontreuil.quizzard.features.cours.FixturesImport;
import fr.iutmontreuil.quizzard.features.cours.FixturesImport.Enseignant;
import fr.iutmontreuil.quizzard.features.cours.PdfDeTest;

/**
 * La limite de taille et la lecture du formulaire sont faites par le serveur HTTP : elles se vérifient avec de vraies
 * requêtes. Les limites sont abaissées pour le test (1 Mo au lieu de 50) ; la sécurité est ouverte, comme en
 * développement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.servlet.multipart.max-file-size=1MB",
        "spring.servlet.multipart.max-request-size=2MB",
        "quizzard.securite.ouverte=true",
        "quizzard.dev.identifiant-enseignant=" + ImportTailleMaximaleTest.IDENTIFIANT})
@Import(TestcontainersConfiguration.class)
class ImportTailleMaximaleTest {

    static final String IDENTIFIANT = "enseignant.taille";
    private static final String SEPARATEUR = "----limite";
    private static final String TITRE = "Réseaux : débit élevé";
    private static final String NOM_FICHIER = "Réseaux évalués.pdf";

    @TempDir
    static Path stockage;

    @DynamicPropertySource
    static void dossierDeStockage(DynamicPropertyRegistry proprietes) {
        proprietes.add("quizzard.stockage.dossier", stockage::toString);
    }

    @LocalServerPort
    int port;
    @Autowired
    JdbcClient jdbc;

    FixturesImport fixtures;
    Enseignant enseignant;

    @BeforeEach
    void preparer() {
        fixtures = new FixturesImport(jdbc);
        enseignant = fixtures.creerEnseignant(IDENTIFIANT);
    }

    @AfterEach
    void nettoyer() {
        fixtures.nettoyer();
    }

    @Test
    void un_fichier_sous_la_limite_est_importe_avec_ses_accents() throws Exception {
        HttpResponse<String> reponse = envoyer(PdfDeTest.pages(1));

        assertThat(reponse.statusCode()).isEqualTo(201);
        assertThat(reponse.body()).contains("\"titre\":\"" + TITRE + "\"");
        assertThat(jdbc.sql("SELECT v.nom_fichier FROM version_cours v JOIN cours c ON c.id_cours = v.id_cours "
                        + "WHERE c.id_enseignant = :id").param("id", enseignant.id()).query(String.class).single())
                .isEqualTo(NOM_FICHIER);
    }

    @Test
    void un_fichier_au_dela_de_la_limite_repond_413() throws Exception {
        for (int megaoctets : new int[] {1, 8}) {
            byte[] tropGros = Arrays.copyOf(PdfDeTest.pages(1), megaoctets * 1024 * 1024 + 10);

            HttpResponse<String> reponse = envoyer(tropGros);

            assertThat(reponse.statusCode()).as(megaoctets + " Mo").isEqualTo(413);
            assertThat(reponse.body()).contains("Le fichier envoyé est trop volumineux.", "request_id");
        }
        assertThat(fixtures.nombreDeCours(enseignant)).isZero();
    }

    private HttpResponse<String> envoyer(byte[] fichier) throws IOException, InterruptedException {
        ByteArrayOutputStream corps = new ByteArrayOutputStream();
        corps.write(("--" + SEPARATEUR + "\r\n"
                + "Content-Disposition: form-data; name=\"titre\"\r\n\r\n"
                + TITRE + "\r\n"
                + "--" + SEPARATEUR + "\r\n"
                + "Content-Disposition: form-data; name=\"fichier\"; filename=\"" + NOM_FICHIER + "\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        corps.write(fichier);
        corps.write(("\r\n--" + SEPARATEUR + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest requete = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/cours"))
                .header("Content-Type", "multipart/form-data; boundary=" + SEPARATEUR)
                .POST(HttpRequest.BodyPublishers.ofByteArray(corps.toByteArray()))
                .build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(requete, HttpResponse.BodyHandlers.ofString());
        }
    }
}
