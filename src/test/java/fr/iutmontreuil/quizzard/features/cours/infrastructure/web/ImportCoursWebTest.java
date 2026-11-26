package fr.iutmontreuil.quizzard.features.cours.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;
import fr.iutmontreuil.quizzard.features.cours.FixturesImport;
import fr.iutmontreuil.quizzard.features.cours.FixturesImport.Enseignant;
import fr.iutmontreuil.quizzard.features.cours.PdfDeTest;
import fr.iutmontreuil.quizzard.features.cours.application.StockageFichierException;
import fr.iutmontreuil.quizzard.features.cours.application.port.StockageFichiers;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/**
 * Import d'un cours de bout en bout (contrôleur, cas d'usage, disque, base PostgreSQL) : réponse, écritures, tableau
 * des cas d'erreur du dossier, tout ou rien et sécurité.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class ImportCoursWebTest {

    @TempDir
    static Path stockage;

    @DynamicPropertySource
    static void dossierDeStockage(DynamicPropertyRegistry proprietes) {
        proprietes.add("quizzard.stockage.dossier", stockage::toString);
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcClient jdbc;
    @MockitoSpyBean
    StockageFichiers fichiers;
    @MockitoSpyBean
    JournalEvenements journal;

    FixturesImport fixtures;
    Enseignant enseignant;

    @BeforeEach
    void preparer() throws IOException {
        viderLeStockage();
        fixtures = new FixturesImport(jdbc);
        enseignant = fixtures.creerEnseignant();
    }

    @AfterEach
    void nettoyer() {
        fixtures.nettoyer();
    }

    @Test
    void importer_cree_le_cours_sa_version_et_le_fichier(CapturedOutput logs) throws Exception {
        byte[] pdf = PdfDeTest.pages(3);

        ResultActions reponse = importer(enseignant, "  Titre confidentiel  ", "Dupont-chapitre.pdf", pdf)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titre").value("Titre confidentiel"))
                .andExpect(jsonPath("$.numeroVersion").value(1))
                .andExpect(jsonPath("$.nbPages").value(3))
                .andExpect(jsonPath("$.nbQuiz").value(0));
        String corps = reponse.andReturn().getResponse().getContentAsString();
        int idCours = JsonPath.read(corps, "$.idCours");
        reponse.andExpect(header().string("Location", "/api/cours/" + idCours));
        assertThat(LocalDateTime.parse(JsonPath.read(corps, "$.dateModification"))).isNotNull();

        assertThat(jdbc.sql("SELECT * FROM cours WHERE id_cours = :id").param("id", idCours).query().singleRow())
                .containsEntry("titre", "Titre confidentiel").containsEntry("archive", false)
                .containsEntry("id_enseignant", enseignant.id());
        Map<String, Object> version = jdbc.sql("SELECT * FROM version_cours WHERE id_cours = :id")
                .param("id", idCours).query().singleRow();
        assertThat(version).containsEntry("numero", 1).containsEntry("nom_fichier", "Dupont-chapitre.pdf")
                .containsEntry("nb_pages", 3).containsEntry("taille_octets", (long) pdf.length)
                .containsEntry("empreinte", HexFormat.of().formatHex(sha256(pdf)))
                .containsEntry("date_suppression_fichier", null);
        String chemin = (String) version.get("chemin_stockage");
        assertThat(chemin).matches("cours/[0-9a-f-]{36}\\.pdf");
        assertThat(stockage.resolve(chemin)).hasBinaryContent(pdf);
        assertThat(fichiersStockes()).containsExactly(stockage.resolve(chemin));

        Map<String, Object> trace = jdbc.sql(
                        "SELECT * FROM journal_evenement WHERE type_action = 'cours_importe' AND cible = :cible")
                .param("cible", String.valueOf(idCours)).query().singleRow();
        assertThat(trace).containsEntry("acteur", enseignant.identifiant());
        assertThat(String.valueOf(trace.get("details"))).contains("nbPages").doesNotContain("confidentiel");
        assertThat(logs.getOut()).contains("cours_importe")
                .doesNotContain("Titre confidentiel").doesNotContain("Dupont");
    }

    @Test
    void sans_connexion_401_et_sans_jeton_csrf_403() throws Exception {
        mvc.perform(requete("Réseaux", PdfDeTest.pages(1)).with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Vous devez être connecté."));
        mvc.perform(requete("Réseaux", PdfDeTest.pages(1)).with(user(enseignant.identifiant())))
                .andExpect(status().isForbidden());

        rienNAEteCree();
    }

    @Test
    void une_requete_sans_fichier_ou_mal_formee_est_refusee() throws Exception {
        mvc.perform(multipart("/api/cours").param("titre", "Réseaux")
                        .with(csrf()).with(user(enseignant.identifiant())))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/cours").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .with(csrf()).with(user(enseignant.identifiant())))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(requete("Réseaux", PdfDeTest.pages(1)).param("confirmerDoublon", "peut-être")
                        .with(csrf()).with(user(enseignant.identifiant())))
                .andExpect(status().isBadRequest());

        rienNAEteCree();
    }

    @Test
    void un_titre_absent_vide_ou_trop_long_repond_422() throws Exception {
        for (String titre : new String[] {null, "   ", "t".repeat(201)}) {
            importer(enseignant, titre, "cours.pdf", PdfDeTest.pages(1))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("TITRE_INVALIDE"))
                    .andExpect(jsonPath("$.champs.titre")
                            .value("Le titre est obligatoire (200 caractères au plus)."));
        }
        rienNAEteCree();

        importer(enseignant, "t".repeat(200), "cours.pdf", PdfDeTest.pages(1)).andExpect(status().isCreated());
    }

    @Test
    void un_fichier_qui_n_est_pas_un_pdf_repond_415_malgre_son_nom_et_son_type() throws Exception {
        importer(enseignant, "Réseaux", "cours.pdf", "du texte".getBytes())
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("FICHIER_NON_PDF"))
                .andExpect(jsonPath("$.error").value("Le fichier n'est pas un PDF."));
        importer(enseignant, "Réseaux", "vide.pdf", new byte[0]).andExpect(status().isUnsupportedMediaType());

        rienNAEteCree();
    }

    @Test
    void un_pdf_corrompu_chiffre_ou_sans_page_repond_422_sans_recopier_son_contenu(CapturedOutput logs)
            throws Exception {
        for (byte[] pdf : List.of(PdfDeTest.corrompu("FRAGMENT-SECRET"), PdfDeTest.chiffre("secret"),
                PdfDeTest.pages(0))) {
            importer(enseignant, "Réseaux", "cours.pdf", pdf)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("PDF_ILLISIBLE"))
                    .andExpect(jsonPath("$.error")
                            .value("Le PDF est illisible ou protégé par un mot de passe."));
        }

        rienNAEteCree();
        assertThat(logs.getOut()).doesNotContain("FRAGMENT-SECRET");
    }

    @Test
    void un_pdf_deja_importe_demande_une_confirmation() throws Exception {
        byte[] pdf = PdfDeTest.pages(2);
        importer(enseignant, "Réseaux", "cours.pdf", pdf).andExpect(status().isCreated());

        importer(enseignant, "Autre titre", "autre-nom.pdf", pdf)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOUBLON_COURS"))
                .andExpect(jsonPath("$.error").value(
                        "Vous avez déjà importé ce PDF dans le cours « Réseaux ». Importer quand même ?"));
        assertThat(fixtures.nombreDeCours(enseignant)).isEqualTo(1);
        assertThat(fichiersStockes()).hasSize(1);

        mvc.perform(requete("Autre titre", pdf).param("confirmerDoublon", "true")
                        .with(csrf()).with(user(enseignant.identifiant())))
                .andExpect(status().isCreated());
        assertThat(fixtures.nombreDeCours(enseignant)).isEqualTo(2);
        assertThat(fichiersStockes()).hasSize(2);
    }

    @Test
    void le_doublon_ne_concerne_que_les_cours_non_archives_du_meme_enseignant() throws Exception {
        byte[] pdf = PdfDeTest.pages(2);
        importer(enseignant, "Réseaux", "cours.pdf", pdf).andExpect(status().isCreated());

        importer(fixtures.creerEnseignant(), "Réseaux", "cours.pdf", pdf).andExpect(status().isCreated());
        jdbc.sql("UPDATE cours SET archive = TRUE WHERE id_enseignant = :id").param("id", enseignant.id()).update();
        importer(enseignant, "Réseaux", "cours.pdf", pdf).andExpect(status().isCreated());
    }

    @Test
    void une_panne_du_disque_ne_cree_rien() throws Exception {
        doThrow(new StockageFichierException("panne simulée", null)).when(fichiers).deplacer(any(), any());

        importer(enseignant, "Réseaux", "cours.pdf", PdfDeTest.pages(1)).andExpect(status().isInternalServerError());

        rienNAEteCree();
    }

    @Test
    void une_panne_apres_l_ecriture_annule_la_base_et_supprime_le_fichier_range() throws Exception {
        // La trace est la dernière écriture : le cours et sa version sont déjà en base, le fichier déjà rangé.
        doThrow(new DataAccessResourceFailureException("panne simulée"))
                .when(journal).actionSensible(eq("cours_importe"), any());

        importer(enseignant, "Réseaux", "cours.pdf", PdfDeTest.pages(1)).andExpect(status().isInternalServerError());

        rienNAEteCree();
    }

    private ResultActions importer(Enseignant auteur, String titre, String nomFichier, byte[] contenu)
            throws Exception {
        MockMultipartHttpServletRequestBuilder requete = multipart("/api/cours")
                .file(new MockMultipartFile("fichier", nomFichier, "application/pdf", contenu));
        if (titre != null) {
            requete.param("titre", titre);
        }
        return mvc.perform(requete.with(csrf()).with(user(auteur.identifiant())));
    }

    private static MockMultipartHttpServletRequestBuilder requete(String titre, byte[] contenu) {
        MockMultipartHttpServletRequestBuilder requete = multipart("/api/cours")
                .file(new MockMultipartFile("fichier", "cours.pdf", "application/pdf", contenu));
        requete.param("titre", titre);
        return requete;
    }

    private void rienNAEteCree() throws IOException {
        assertThat(fixtures.nombreDeCours(enseignant)).isZero();
        assertThat(fichiersStockes()).isEmpty();
        assertThat(jdbc.sql("SELECT count(*) FROM journal_evenement WHERE type_action = 'cours_importe' "
                        + "AND acteur = :acteur").param("acteur", enseignant.identifiant())
                .query(Integer.class).single()).isZero();
    }

    /** Tous les fichiers du stockage, temporaires compris. */
    private static List<Path> fichiersStockes() throws IOException {
        if (!Files.exists(stockage)) {
            return List.of();
        }
        try (Stream<Path> chemins = Files.walk(stockage)) {
            return chemins.filter(Files::isRegularFile).toList();
        }
    }

    private static void viderLeStockage() throws IOException {
        for (Path fichier : fichiersStockes()) {
            Files.delete(fichier);
        }
    }

    private static byte[] sha256(byte[] contenu) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(contenu);
    }
}
