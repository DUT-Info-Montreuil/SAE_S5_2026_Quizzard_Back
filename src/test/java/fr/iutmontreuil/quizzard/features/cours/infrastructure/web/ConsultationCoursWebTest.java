package fr.iutmontreuil.quizzard.features.cours.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;
import fr.iutmontreuil.quizzard.features.cours.FixturesImport;
import fr.iutmontreuil.quizzard.features.cours.FixturesImport.Enseignant;
import fr.iutmontreuil.quizzard.features.cours.PdfDeTest;

/**
 * Liste et ouverture des cours de bout en bout (contrôleur, cas d'usage, base PostgreSQL) : forme des réponses,
 * dernière version, cours visibles et sécurité.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ConsultationCoursWebTest {

    private static final LocalDateTime DATE = LocalDateTime.of(2026, 11, 25, 14, 0);

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

    FixturesImport fixtures;
    Enseignant enseignant;

    @BeforeEach
    void preparer() {
        fixtures = new FixturesImport(jdbc);
        enseignant = fixtures.creerEnseignant();
    }

    @AfterEach
    void nettoyer() {
        fixtures.nettoyer();
    }

    @Test
    void un_enseignant_sans_cours_recoit_un_tableau_vide() throws Exception {
        lister(enseignant)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void un_cours_importe_apparait_dans_la_liste_avec_la_forme_de_la_reponse_de_l_import() throws Exception {
        String importe = corps(mvc.perform(multipart("/api/cours")
                        .file(new MockMultipartFile("fichier", "cours.pdf", "application/pdf", PdfDeTest.pages(3)))
                        .param("titre", "Réseaux")
                        .with(csrf()).with(user(enseignant.identifiant())))
                .andExpect(status().isCreated()));
        Map<String, Object> cours = JsonPath.read(importe, "$");

        String liste = corps(lister(enseignant).andExpect(status().isOk()));

        assertThat(JsonPath.<List<Map<String, Object>>>read(liste, "$")).containsExactly(cours);
        int idCours = (int) cours.get("idCours");
        int idVersion = jdbc.sql("SELECT id_version FROM version_cours WHERE id_cours = :id")
                .param("id", idCours).query(Integer.class).single();
        Map<String, Object> ouvert = JsonPath.read(
                corps(ouvrir(enseignant, idCours).andExpect(status().isOk())), "$");
        assertThat(ouvert).containsAllEntriesOf(cours).containsEntry("idVersion", idVersion)
                .hasSize(cours.size() + 1);
    }

    @Test
    void la_liste_et_l_ouverture_donnent_la_derniere_version_et_comptent_les_quiz_non_archives() throws Exception {
        Long idCours = fixtures.creerCours(enseignant, "Réseaux", DATE);
        // La version la plus récente est insérée la première : seul son numéro la désigne.
        Long derniere = fixtures.ajouterVersion(idCours, 2, 15);
        fixtures.ajouterVersion(idCours, 1, 12);
        fixtures.ajouterQuiz(idCours, false);
        fixtures.ajouterQuiz(idCours, false);
        fixtures.ajouterQuiz(idCours, true);

        lister(enseignant)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idCours").value(idCours.intValue()))
                .andExpect(jsonPath("$[0].titre").value("Réseaux"))
                .andExpect(jsonPath("$[0].numeroVersion").value(2))
                .andExpect(jsonPath("$[0].nbPages").value(15))
                .andExpect(jsonPath("$[0].nbQuiz").value(2))
                .andExpect(jsonPath("$[0].dateModification").value("2026-11-25T14:00:00"));
        ouvrir(enseignant, idCours)
                .andExpect(jsonPath("$.idCours").value(idCours.intValue()))
                .andExpect(jsonPath("$.titre").value("Réseaux"))
                .andExpect(jsonPath("$.idVersion").value(derniere.intValue()))
                .andExpect(jsonPath("$.numeroVersion").value(2))
                .andExpect(jsonPath("$.nbPages").value(15))
                .andExpect(jsonPath("$.nbQuiz").value(2))
                .andExpect(jsonPath("$.dateModification").value("2026-11-25T14:00:00"));
    }

    @Test
    void les_cours_modifies_le_plus_recemment_sont_en_premier() throws Exception {
        Long ancien = cours(enseignant, DATE.minusDays(1));
        Long recent = cours(enseignant, DATE);
        // À date égale, le dernier créé passe devant.
        Long recentAussi = cours(enseignant, DATE);

        String liste = corps(lister(enseignant).andExpect(status().isOk()));

        assertThat(JsonPath.<List<Integer>>read(liste, "$[*].idCours"))
                .containsExactly(recentAussi.intValue(), recent.intValue(), ancien.intValue());
    }

    @Test
    void la_liste_ne_montre_ni_les_cours_des_autres_enseignants_ni_les_cours_archives() throws Exception {
        Long visible = cours(enseignant, DATE);
        fixtures.archiver(cours(enseignant, DATE));
        cours(fixtures.creerEnseignant(), DATE);

        String liste = corps(lister(enseignant).andExpect(status().isOk()));

        assertThat(JsonPath.<List<Integer>>read(liste, "$[*].idCours")).containsExactly(visible.intValue());
    }

    @Test
    void un_cours_inconnu_archive_ou_d_un_autre_enseignant_repond_le_meme_404() throws Exception {
        Long archive = cours(enseignant, DATE);
        fixtures.archiver(archive);
        Long autrui = cours(fixtures.creerEnseignant(), DATE);

        for (long idCours : new long[] {999_999_999L, archive, autrui}) {
            ouvrir(enseignant, idCours)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("COURS_INTROUVABLE"))
                    .andExpect(jsonPath("$.error").value("Cours introuvable."));
        }
    }

    @Test
    void un_identifiant_de_cours_qui_n_est_pas_un_nombre_repond_400() throws Exception {
        mvc.perform(get("/api/cours/abc").with(user(enseignant.identifiant())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sans_connexion_la_liste_et_l_ouverture_repondent_401() throws Exception {
        Long idCours = cours(enseignant, DATE);

        mvc.perform(get("/api/cours"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Vous devez être connecté."));
        mvc.perform(get("/api/cours/{idCours}", idCours))
                .andExpect(status().isUnauthorized());
    }

    private Long cours(Enseignant auteur, LocalDateTime dateModification) {
        Long idCours = fixtures.creerCours(auteur, "Réseaux", dateModification);
        fixtures.ajouterVersion(idCours, 1, 1);
        return idCours;
    }

    private ResultActions lister(Enseignant auteur) throws Exception {
        return mvc.perform(get("/api/cours").with(user(auteur.identifiant())));
    }

    private ResultActions ouvrir(Enseignant auteur, long idCours) throws Exception {
        return mvc.perform(get("/api/cours/{idCours}", idCours).with(user(auteur.identifiant())));
    }

    private static String corps(ResultActions reponse) throws Exception {
        return reponse.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
