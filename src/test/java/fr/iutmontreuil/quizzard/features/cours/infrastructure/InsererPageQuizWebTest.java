package fr.iutmontreuil.quizzard.features.cours.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;

/**
 * UC3 de bout en bout sur PostgreSQL (sécurité fermée). La connexion (UC1) n'existe pas encore : l'enseignant
 * connecté est simulé par {@code @WithMockUser}, son nom étant son identifiant LDAP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Sql("/cours/cours-a-modifier.sql")
@WithMockUser(username = "enseignant.test")
class InsererPageQuizWebTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    private ResultActions inserer(long idCours, String corps) throws Exception {
        return mvc.perform(post("/api/cours/" + idCours + "/pages-quiz").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    @Test
    void sans_quiz_choisi_la_page_cree_un_quiz_et_une_question_vierge() throws Exception {
        inserer(2000, "{\"idVersion\": 2001, \"numeroPage\": 4, \"ordre\": 0, \"repereTexte\": \"fin  de page\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern(
                        "/api/cours/2000/pages/\\d+")))
                .andExpect(jsonPath("$.quizCree").value(true))
                .andExpect(jsonPath("$.titreQuiz").value("Quiz 3"))
                .andExpect(jsonPath("$.numeroPage").value(4))
                .andExpect(jsonPath("$.ordre").value(0))
                .andExpect(jsonPath("$.idQuestion").isNumber())
                .andExpect(jsonPath("$.pages.length()").value(2))
                .andExpect(jsonPath("$.pages[0].numeroPage").value(2))
                .andExpect(jsonPath("$.pages[1].numeroPage").value(4))
                .andExpect(jsonPath("$.pages[1].type").value("QUIZ"));

        assertThat(jdbc.sql("""
                        SELECT q.type || '|' || q.enonce || '|' || coalesce(q.duree_sec::text, 'null') || '|'
                               || q.points_max || '|' || q.decroissance_temps || '|' || q.points_partiels || '|'
                               || q.verrouillee
                          FROM question q JOIN quiz z USING (id_quiz) WHERE z.titre = 'Quiz 3'
                        """).query(String.class).single())
                .isEqualTo("QCM||null|1000|true|false|false");
        assertThat(jdbc.sql("SELECT repere_texte FROM page_inseree WHERE numero_page = 4").query(String.class)
                .single()).isEqualTo("fin de page");
        assertThat(jdbc.sql("SELECT date_modification > '2026-01-01' FROM cours WHERE id_cours = 2000")
                .query(Boolean.class).single()).isTrue();
    }

    @Test
    void inserer_avant_une_page_existante_la_decale_d_un_rang() throws Exception {
        inserer(2000, "{\"idVersion\": 2001, \"numeroPage\": 2, \"ordre\": 0, \"idQuiz\": 2000}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quizCree").value(false))
                .andExpect(jsonPath("$.idQuiz").value(2000))
                .andExpect(jsonPath("$.titreQuiz").value("Quiz 1"));

        List<String> pages = jdbc.sql("""
                        SELECT id_page || '@' || numero_page || ',' || ordre FROM page_inseree
                         WHERE id_version = 2001 ORDER BY numero_page, ordre
                        """).query(String.class).list();
        assertThat(pages).hasSize(2);
        assertThat(pages.get(1)).isEqualTo("2000@2,1");
        assertThat(jdbc.sql("SELECT count(*) FROM question WHERE id_quiz = 2000").query(Long.class).single())
                .isEqualTo(2);
    }

    @Test
    void un_cours_d_un_autre_enseignant_repond_comme_un_cours_inexistant() throws Exception {
        inserer(2002, "{\"idVersion\": 2003, \"numeroPage\": 1, \"ordre\": 0}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURS_INTROUVABLE"))
                .andExpect(jsonPath("$.error").value("Cours introuvable."));
        inserer(9999, "{\"idVersion\": 2003, \"numeroPage\": 1, \"ordre\": 0}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURS_INTROUVABLE"));
    }

    @Test
    void un_cours_archive_refuse_l_insertion() throws Exception {
        inserer(2001, "{\"idVersion\": 2002, \"numeroPage\": 1, \"ordre\": 0}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COURS_ARCHIVE"))
                .andExpect(jsonPath("$.error").value("Ce cours est archivé : il n'est plus modifiable."));
    }

    @Test
    void une_version_perimee_est_refusee() throws Exception {
        inserer(2000, "{\"idVersion\": 2000, \"numeroPage\": 1, \"ordre\": 0}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_PERIMEE"));
    }

    @Test
    void un_emplacement_hors_du_cours_est_refuse_et_rien_n_est_cree() throws Exception {
        inserer(2000, "{\"idVersion\": 2001, \"numeroPage\": 13, \"ordre\": 0}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("EMPLACEMENT_INVALIDE"))
                .andExpect(jsonPath("$.error").value("Emplacement invalide : rechargez l'éditeur."));
        inserer(2000, "{\"idVersion\": 2001, \"numeroPage\": 2, \"ordre\": 2}")
                .andExpect(status().isUnprocessableContent());
        assertThat(jdbc.sql("SELECT count(*) FROM quiz WHERE id_cours = 2000").query(Long.class).single())
                .isEqualTo(2);
    }

    @Test
    void un_quiz_d_un_autre_cours_est_introuvable() throws Exception {
        inserer(2000, "{\"idVersion\": 2001, \"numeroPage\": 1, \"ordre\": 0, \"idQuiz\": 2002}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUIZ_INTROUVABLE"));
    }

    @Test
    void un_quiz_en_cours_de_seance_refuse_l_insertion() throws Exception {
        inserer(2000, "{\"idVersion\": 2001, \"numeroPage\": 1, \"ordre\": 0, \"idQuiz\": 2001}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUIZ_EN_SEANCE"));
    }

    @Test
    void un_corps_incomplet_ou_negatif_est_refuse_avec_requete_invalide() throws Exception {
        inserer(2000, "{\"idVersion\": 2001, \"numeroPage\": 1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"))
                .andExpect(jsonPath("$.champs.ordre").exists());
        inserer(2000, "{\"idVersion\": 2001, \"numeroPage\": -1, \"ordre\": 0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.numeroPage").exists());
        inserer(2000, "{\"idVersion\": 2001, \"numeroPage\": 1, \"ordre\": 0, \"repereTexte\": \""
                + "a".repeat(301) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champs.repereTexte").exists());
    }

    @Test
    @WithMockUser(username = "inconnu")
    void un_utilisateur_qui_n_est_pas_un_enseignant_connu_est_refuse() throws Exception {
        inserer(2000, "{\"idVersion\": 2001, \"numeroPage\": 1, \"ordre\": 0}")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sans_connexion_la_route_repond_401() throws Exception {
        mvc.perform(post("/api/cours/2000/pages-quiz").with(csrf())
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idVersion\": 2001, \"numeroPage\": 1, \"ordre\": 0}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", containsString("json")));
    }
}
