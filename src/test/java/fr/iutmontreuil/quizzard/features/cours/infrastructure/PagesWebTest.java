package fr.iutmontreuil.quizzard.features.cours.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
 * Éditeur E3 de bout en bout sur PostgreSQL : lister, retirer et déplacer les pages insérées d'une version.
 * L'enseignant connecté est simulé ({@code @WithMockUser}, nom = identifiant LDAP).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Sql({"/cours/cours-a-modifier.sql", "/cours/pages-a-modifier.sql"})
@WithMockUser(username = "enseignant.test")
class PagesWebTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    /** Pages de la version 2001 : « idPage@page,rang », dans l'ordre. */
    private List<String> pages() {
        return jdbc.sql("""
                SELECT id_page || '@' || numero_page || ',' || ordre FROM page_inseree
                 WHERE id_version = 2001 ORDER BY numero_page, ordre
                """).query(String.class).list();
    }

    private ResultActions deplacer(long idPage, String corps) throws Exception {
        return mvc.perform(patch("/api/pages/" + idPage).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private ResultActions retirer(long idPage) throws Exception {
        return mvc.perform(delete("/api/pages/" + idPage).with(csrf()));
    }

    // ---- Lister ----

    @Test
    void la_liste_donne_les_pages_dans_l_ordre_avec_leur_quiz_et_leur_question() throws Exception {
        mvc.perform(get("/api/cours/2000/versions/2001/pages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].idPage").value(2000))
                .andExpect(jsonPath("$[0].numeroPage").value(2))
                .andExpect(jsonPath("$[0].ordre").value(0))
                .andExpect(jsonPath("$[0].type").value("QUIZ"))
                .andExpect(jsonPath("$[0].idQuestion").value(2000))
                .andExpect(jsonPath("$[0].idQuiz").value(2000))
                .andExpect(jsonPath("$[0].titreQuiz").value("Quiz 1"))
                .andExpect(jsonPath("$[1].idPage").value(2001))
                .andExpect(jsonPath("$[3].titreQuiz").value("Quiz en séance"));
    }

    @Test
    void une_ancienne_version_du_cours_se_lit_aussi() throws Exception {
        mvc.perform(get("/api/cours/2000/versions/2000/pages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void la_liste_d_un_cours_d_un_autre_enseignant_ou_d_une_version_etrangere_est_introuvable() throws Exception {
        mvc.perform(get("/api/cours/2002/versions/2003/pages"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURS_INTROUVABLE"));
        mvc.perform(get("/api/cours/2000/versions/2003/pages"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VERSION_INTROUVABLE"));
    }

    // ---- Retirer ----

    @Test
    void retirer_une_page_garde_sa_question_et_recale_les_pages_au_meme_endroit() throws Exception {
        long journalAvant = retraitsJournalises();
        retirer(2000).andExpect(status().isNoContent()).andExpect(content().string(""));

        assertThat(pages()).containsExactly("2001@2,0", "2002@5,0", "2003@7,0");
        assertThat(jdbc.sql("SELECT count(*) FROM question WHERE id_question = 2000").query(Long.class).single())
                .isEqualTo(1);
        assertThat(retraitsJournalises()).isEqualTo(journalAvant + 1);
    }

    /** Le journal n'est pas vidé entre les tests : on compte ce que le retrait ajoute. */
    private long retraitsJournalises() {
        return jdbc.sql("SELECT count(*) FROM journal_evenement WHERE type_action = 'page_quiz_retiree'")
                .query(Long.class).single();
    }

    @Test
    void retirer_la_derniere_page_d_un_quiz_conserve_le_quiz() throws Exception {
        retirer(2000).andExpect(status().isNoContent());
        retirer(2001).andExpect(status().isNoContent());
        retirer(2002).andExpect(status().isNoContent());
        assertThat(jdbc.sql("SELECT archive FROM quiz WHERE id_quiz = 2000").query(Boolean.class).single()).isFalse();
    }

    @Test
    void une_page_inexistante_ou_d_un_autre_enseignant_est_introuvable() throws Exception {
        retirer(9999).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PAGE_INTROUVABLE"));
        retirer(2005).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PAGE_INTROUVABLE"));
        deplacer(2005, "{\"numeroPage\": 1, \"ordre\": 0}").andExpect(status().isNotFound());
    }

    @Test
    void une_page_d_un_cours_archive_d_une_ancienne_version_ou_d_un_quiz_en_seance_ne_se_modifie_pas()
            throws Exception {
        retirer(2006).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COURS_ARCHIVE"));
        retirer(2004).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VERSION_PERIMEE"));
        retirer(2003).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("QUIZ_EN_SEANCE"));
        deplacer(2003, "{\"numeroPage\": 1, \"ordre\": 0}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("QUIZ_EN_SEANCE"));
        assertThat(pages()).hasSize(4);
    }

    // ---- Déplacer ----

    @Test
    void deplacer_une_page_ailleurs_recale_l_ancien_et_le_nouvel_endroit() throws Exception {
        deplacer(2000, "{\"numeroPage\": 5, \"ordre\": 0}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].idPage").value(2001))
                .andExpect(jsonPath("$[0].ordre").value(0))
                .andExpect(jsonPath("$[1].idPage").value(2000))
                .andExpect(jsonPath("$[1].numeroPage").value(5));

        assertThat(pages()).containsExactly("2001@2,0", "2000@5,0", "2002@5,1", "2003@7,0");
    }

    @Test
    void deplacer_une_page_au_meme_endroit_la_met_au_rang_demande_parmi_les_autres() throws Exception {
        deplacer(2000, "{\"numeroPage\": 2, \"ordre\": 1}").andExpect(status().isOk());
        assertThat(pages()).containsExactly("2001@2,0", "2000@2,1", "2002@5,0", "2003@7,0");
    }

    @Test
    void deplacer_avant_la_premiere_page_ou_apres_la_derniere_est_permis() throws Exception {
        deplacer(2002, "{\"numeroPage\": 0, \"ordre\": 0}").andExpect(status().isOk());
        deplacer(2001, "{\"numeroPage\": 12, \"ordre\": 0}").andExpect(status().isOk());
        assertThat(pages()).containsExactly("2002@0,0", "2000@2,0", "2003@7,0", "2001@12,0");
    }

    @Test
    void un_emplacement_hors_du_cours_est_refuse_sans_rien_changer() throws Exception {
        deplacer(2000, "{\"numeroPage\": 13, \"ordre\": 0}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("EMPLACEMENT_INVALIDE"));
        // À la page 2, une seule autre page : rangs possibles 0 et 1.
        deplacer(2000, "{\"numeroPage\": 2, \"ordre\": 2}")
                .andExpect(status().isUnprocessableContent());
        assertThat(pages()).containsExactly("2000@2,0", "2001@2,1", "2002@5,0", "2003@7,0");
    }

    @Test
    void un_corps_incomplet_est_refuse_avec_requete_invalide() throws Exception {
        deplacer(2000, "{\"numeroPage\": 2}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"))
                .andExpect(jsonPath("$.champs.ordre").exists());
    }
}
