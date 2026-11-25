package fr.iutmontreuil.quizzard.features.cours.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;

/**
 * PDF d'une version pour PDF.js (éditeur E3), de bout en bout : vrai fichier dans un dossier de stockage temporaire,
 * propriétaire vérifié, en-têtes, lecture par tranches et chemins qui sortent du stockage.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Sql("/cours/cours-a-modifier.sql")
@WithMockUser(username = "enseignant.test")
class FichierVersionWebTest {

    private static final byte[] PDF = "%PDF-1.4\n% contenu de test du cours\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
    private static final String ROUTE = "/api/cours/2000/versions/2001/fichier";

    @TempDir
    static Path stockage;

    @DynamicPropertySource
    static void dossierDeStockage(DynamicPropertyRegistry proprietes) {
        proprietes.add("quizzard.stockage.dossier", () -> stockage.resolve("racine").toString());
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void rangerLePdf() throws IOException {
        Path cours = Files.createDirectories(stockage.resolve("racine/cours"));
        Files.write(cours.resolve("v2.pdf"), PDF);
        Files.write(stockage.resolve("secret.pdf"), PDF);
        cheminDeLaVersion("cours/v2.pdf");
    }

    private void cheminDeLaVersion(String chemin) {
        jdbc.sql("UPDATE version_cours SET chemin_stockage = :c WHERE id_version = 2001").param("c", chemin).update();
    }

    @Test
    void le_pdf_est_servi_en_ligne_avec_les_en_tetes_demandes() throws Exception {
        MvcResult reponse = mvc.perform(get(ROUTE))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "inline"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", "private"))
                .andReturn();
        assertThat(reponse.getResponse().getContentAsByteArray()).isEqualTo(PDF);
    }

    @Test
    void une_requete_range_renvoie_seulement_la_tranche_demandee() throws Exception {
        mvc.perform(get(ROUTE).header("Range", "bytes=0-7"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 0-7/" + PDF.length))
                .andExpect(content().string("%PDF-1.4"));
    }

    @Test
    void un_cours_inexistant_ou_d_un_autre_enseignant_est_introuvable() throws Exception {
        mvc.perform(get("/api/cours/2002/versions/2003/fichier"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURS_INTROUVABLE"));
        mvc.perform(get("/api/cours/9999/versions/2001/fichier"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURS_INTROUVABLE"));
    }

    @Test
    void une_version_d_un_autre_cours_est_introuvable() throws Exception {
        mvc.perform(get("/api/cours/2000/versions/2003/fichier"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VERSION_INTROUVABLE"));
    }

    @Test
    void un_fichier_supprime_par_l_administrateur_est_introuvable() throws Exception {
        jdbc.sql("UPDATE version_cours SET chemin_stockage = NULL, date_suppression_fichier = LOCALTIMESTAMP "
                + "WHERE id_version = 2001").update();
        mvc.perform(get(ROUTE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FICHIER_INTROUVABLE"));
    }

    @Test
    void un_fichier_absent_du_disque_est_introuvable() throws Exception {
        cheminDeLaVersion("cours/absent.pdf");
        mvc.perform(get(ROUTE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FICHIER_INTROUVABLE"));
    }

    @Test
    void un_chemin_qui_sort_du_dossier_de_stockage_n_est_jamais_lu() throws Exception {
        cheminDeLaVersion("../secret.pdf");
        mvc.perform(get(ROUTE)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FICHIER_INTROUVABLE"));
        cheminDeLaVersion(stockage.resolve("secret.pdf").toString());
        mvc.perform(get(ROUTE)).andExpect(status().isNotFound());
    }
}
