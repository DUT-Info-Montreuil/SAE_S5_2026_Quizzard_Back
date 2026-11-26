package fr.iutmontreuil.quizzard.features.seance.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;

/**
 * UC9 de bout en bout : routes REST, cas d'usage, adaptateurs PostgreSQL (Testcontainers) et format des erreurs.
 * Sécurité fermée (réglage par défaut) : les routes publiques doivent répondre sans session ni jeton CSRF.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Sql("/seance/seance-en-cours.sql")
class RepondreQuestionWebTest {

    private static final String JETON = "jeton-de-test-uc9";
    private static final String REPONSES = "/api/public/sessions/QUIZ-UC9/reponses";
    private static final String ETAT = "/api/public/sessions/QUIZ-UC9/etat";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    Clock horloge;

    @BeforeEach
    void ouvrirLaQuestion() {
        ouvrir(1, LocalDateTime.now(horloge).plusSeconds(15));
    }

    private void ouvrir(int position, LocalDateTime echeance) {
        jdbc.sql("UPDATE session_quiz SET position_courante = :p, echeance_question = :e WHERE id_session = 1000")
                .param("p", position).param("e", echeance).update();
    }

    private ResultActions repondre(String corps) throws Exception {
        return repondre(JETON, corps);
    }

    private ResultActions repondre(String jeton, String corps) throws Exception {
        var requete = post(REPONSES).contentType(MediaType.APPLICATION_JSON).content(corps);
        if (jeton != null) {
            requete.header("X-Jeton-Participant", jeton);
        }
        return mvc.perform(requete);
    }

    private int score() {
        return jdbc.sql("SELECT score_total FROM participant WHERE id_participant = 1000").query(Integer.class)
                .single();
    }

    @Test
    void une_bonne_reponse_au_qcm_est_enregistree_sans_reveler_les_points() throws Exception {
        repondre("{\"idQuestion\": 1000, \"idPropositions\": [1000]}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.idQuestion").value(1000))
                .andExpect(jsonPath("$.horodatageServeur").isNotEmpty())
                .andExpect(jsonPath("$.pointsProvisoires").doesNotExist());

        // Question de 20 s ouverte depuis environ 5 s : entre 500 et 1000 points, ajoutés au score.
        int points = jdbc.sql("SELECT points_calcules FROM reponse WHERE id_participant = 1000")
                .query(Integer.class).single();
        assertThat(points).isBetween(800, 900);
        assertThat(score()).isEqualTo(points);
        assertThat(jdbc.sql("SELECT id_proposition FROM reponse_choix").query(Long.class).list())
                .containsExactly(1000L);
    }

    @Test
    void une_seconde_reponse_est_refusee_avec_deja_repondu() throws Exception {
        repondre("{\"idQuestion\": 1000, \"idPropositions\": [1000]}").andExpect(status().isCreated());
        repondre("{\"idQuestion\": 1000, \"idPropositions\": [1001]}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_REPONDU"))
                .andExpect(jsonPath("$.error").value("Tu as déjà répondu."))
                .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    void une_reponse_apres_l_echeance_est_refusee_avec_hors_delai() throws Exception {
        ouvrir(1, LocalDateTime.now(horloge).minusSeconds(1));
        repondre("{\"idQuestion\": 1000, \"idPropositions\": [1000]}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("HORS_DELAI"))
                .andExpect(jsonPath("$.error").value("Temps écoulé : réponse non prise en compte."));
        assertThat(score()).isZero();
    }

    @Test
    void une_reponse_a_une_question_qui_n_est_pas_ouverte_est_refusee_avec_question_close() throws Exception {
        repondre("{\"idQuestion\": 1001, \"liens\": [{\"gauche\": 1000, \"droite\": 1000}]}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUESTION_CLOSE"))
                .andExpect(jsonPath("$.error").value("Cette session est terminée."));
    }

    @Test
    void une_seance_close_refuse_la_reponse_avec_question_close() throws Exception {
        jdbc.sql("UPDATE session_quiz SET etat = 'CLOTUREE' WHERE id_session = 1000").update();
        repondre("{\"idQuestion\": 1000, \"idPropositions\": [1000]}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUESTION_CLOSE"));
    }

    @Test
    void un_jeton_absent_ou_inconnu_est_refuse_avec_jeton_invalide() throws Exception {
        repondre(null, "{\"idQuestion\": 1000, \"idPropositions\": [1000]}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("JETON_INVALIDE"))
                .andExpect(jsonPath("$.error").value("Ta session a expiré, rejoins le quiz à nouveau."));
        repondre("jeton-inconnu", "{\"idQuestion\": 1000, \"idPropositions\": [1000]}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("JETON_INVALIDE"));
    }

    @Test
    void un_contenu_incoherent_est_refuse_avec_reponse_invalide() throws Exception {
        repondre("{\"idQuestion\": 1000, \"idPropositions\": [1000, 1001]}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("REPONSE_INVALIDE"))
                .andExpect(jsonPath("$.error").value("Réponse incomplète, vérifie ta saisie."));
        repondre("{\"idQuestion\": 1000, \"texteLibre\": \"LDAP\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("REPONSE_INVALIDE"));
        assertThat(score()).isZero();
    }

    @Test
    void un_corps_illisible_est_refuse_avec_400() throws Exception {
        repondre("{pas du json").andExpect(status().isBadRequest());
    }

    @Test
    void une_reponse_sans_question_est_refusee_avec_requete_invalide() throws Exception {
        repondre("{\"idPropositions\": [1000]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"))
                .andExpect(jsonPath("$.champs.idQuestion").exists());
    }

    @Test
    void relier_enregistre_les_liens_et_accorde_les_points_partiels() throws Exception {
        ouvrir(2, LocalDateTime.now(horloge).plusSeconds(30));
        repondre("""
                {"idQuestion": 1001, "liens": [{"gauche": 1000, "droite": 1000}, {"gauche": 1001, "droite": 1002},
                                               {"gauche": 1002, "droite": 1001}]}
                """)
                .andExpect(status().isCreated());
        // 1 paire juste sur 3, réponse quasi instantanée : environ 333 points.
        assertThat(score()).isBetween(320, 334);
        assertThat(jdbc.sql("SELECT count(*) FROM reponse_lien").query(Long.class).single()).isEqualTo(3);
    }

    @Test
    void une_reponse_libre_acceptee_par_un_motif_regex_est_acceptee_par_motif() throws Exception {
        ouvrir(3, LocalDateTime.now(horloge).plusSeconds(20));
        repondre("{\"idQuestion\": 1002, \"texteLibre\": \"  Protocole   LDAPS \"}").andExpect(status().isCreated());
        assertThat(jdbc.sql("SELECT statut_correction FROM reponse").query(String.class).single())
                .isEqualTo("ACCEPTEE_PAR_MOTIF");
        assertThat(score()).isGreaterThan(900);
    }

    @Test
    void une_reponse_libre_sans_motif_correspondant_est_a_examiner_et_vaut_0() throws Exception {
        ouvrir(3, LocalDateTime.now(horloge).plusSeconds(20));
        repondre("{\"idQuestion\": 1002, \"texteLibre\": \"SMTP\"}").andExpect(status().isCreated());
        assertThat(jdbc.sql("SELECT statut_correction FROM reponse").query(String.class).single())
                .isEqualTo("A_EXAMINER");
        assertThat(score()).isZero();
    }

    @Test
    void l_etat_donne_la_question_ouverte_sans_les_reponses_attendues() throws Exception {
        mvc.perform(get(ETAT).header("X-Jeton-Participant", JETON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dejaRepondu").value(false))
                .andExpect(jsonPath("$.scoreTotal").value(0))
                .andExpect(jsonPath("$.question.idQuestion").value(1000))
                .andExpect(jsonPath("$.question.position").value(1))
                .andExpect(jsonPath("$.question.type").value("QCM"))
                .andExpect(jsonPath("$.question.dureeSec").value(20))
                .andExpect(jsonPath("$.question.choixMultiple").value(false))
                .andExpect(jsonPath("$.question.propositions", hasSize(3)))
                .andExpect(jsonPath("$.question.propositions[0].libelle").value("LDAP"))
                .andExpect(jsonPath("$.question.elementsGauche").doesNotExist())
                // L'échéance part avec son décalage horaire (+01:00, Z…), jamais sans fuseau.
                .andExpect(jsonPath("$.question.echeance").value(org.hamcrest.Matchers.matchesPattern(
                        ".*(Z|[+-]\\d{2}:\\d{2})$")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("estCorrecte"))));
    }

    @Test
    void l_etat_d_une_question_a_relier_donne_les_deux_colonnes_sans_les_paires() throws Exception {
        ouvrir(2, LocalDateTime.now(horloge).plusSeconds(30));
        mvc.perform(get(ETAT).header("X-Jeton-Participant", JETON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question.type").value("POINTS_A_RELIER"))
                .andExpect(jsonPath("$.question.elementsGauche[*].libelle", containsInAnyOrder("80", "53", "22")))
                .andExpect(jsonPath("$.question.elementsDroite[*].libelle",
                        containsInAnyOrder("HTTP", "DNS", "SSH")))
                .andExpect(jsonPath("$.question.propositions").doesNotExist());
    }

    @Test
    void apres_une_reponse_l_etat_indique_deja_repondu_et_le_score() throws Exception {
        repondre("{\"idQuestion\": 1000, \"idPropositions\": [1000]}").andExpect(status().isCreated());
        mvc.perform(get(ETAT).header("X-Jeton-Participant", JETON))
                .andExpect(jsonPath("$.dejaRepondu").value(true))
                .andExpect(jsonPath("$.scoreTotal").value(score()));
    }

    @Test
    void sans_question_ouverte_l_etat_renvoie_une_question_nulle() throws Exception {
        jdbc.sql("UPDATE session_quiz SET etat = 'PREPAREE', position_courante = 0, echeance_question = NULL "
                + "WHERE id_session = 1000").update();
        mvc.perform(get(ETAT).header("X-Jeton-Participant", JETON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").doesNotExist())
                .andExpect(jsonPath("$.dejaRepondu").value(false));
    }

    @Test
    void l_etat_sans_jeton_est_refuse_avec_jeton_invalide() throws Exception {
        mvc.perform(get(ETAT))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("JETON_INVALIDE"));
    }

    @Test
    void la_route_ne_renvoie_pas_le_pseudonyme_du_participant() throws Exception {
        mvc.perform(get(ETAT).header("X-Jeton-Participant", JETON))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Alice"))))
                .andExpect(header().string("Cache-Control", endsWith("no-store")));
    }
}
