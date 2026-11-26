package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;
import fr.iutmontreuil.quizzard.features.seance.FixturesLancement;
import fr.iutmontreuil.quizzard.features.seance.FixturesLancement.Enseignant;
import fr.iutmontreuil.quizzard.features.seance.FixturesLancement.QuizDeTest;
import fr.iutmontreuil.quizzard.features.seance.application.ReprendreEcheancesUseCase;

/**
 * Déroulement d'une séance de bout en bout : commandes de l'enseignant, état relu, clôture à l'échéance, passage
 * automatique et reprise après un redémarrage.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DeroulementWebTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcClient jdbc;
    @Autowired
    ReprendreEcheancesUseCase reprendreEcheances;

    FixturesLancement fixtures;
    QuizDeTest quiz;

    @BeforeEach
    void preparer() {
        fixtures = new FixturesLancement(jdbc);
        quiz = fixtures.creerQuiz(false);
    }

    @AfterEach
    void nettoyer() {
        fixtures.nettoyer();
    }

    @Test
    void la_seance_se_deroule_question_par_question_jusqu_a_sa_fin() throws Exception {
        Long qcm = fixtures.ajouterQcm(quiz, 2, "Quel protocole ?", null);
        fixtures.ajouterRelier(quiz, 4);
        fixtures.ajouterLibre(quiz, 6);
        int id = lancer();

        consulter(id)
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.statut").value("PREPAREE"))
                .andExpect(jsonPath("$.position").value(0))
                .andExpect(jsonPath("$.lienAcces").exists())
                .andExpect(jsonPath("$.qrCode").exists())
                .andExpect(jsonPath("$.question").doesNotExist());

        commander(id, "questions/suivante")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_COURS"))
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.questionOuverte").value(true))
                .andExpect(jsonPath("$.question.idQuestion").value(qcm.intValue()))
                .andExpect(jsonPath("$.question.dureeSec").value(20))
                .andExpect(jsonPath("$.question.echeance").exists())
                .andExpect(jsonPath("$.question.choixMultiple").value(false))
                .andExpect(jsonPath("$.question.propositions", hasSize(2)))
                .andExpect(jsonPath("$.question.propositions[0].estCorrecte").doesNotExist())
                .andExpect(jsonPath("$.correction").doesNotExist())
                .andExpect(jsonPath("$.tempsRestantMs").isNumber());

        Long premier = fixtures.ajouterParticipant((long) id, "Un", 900);
        fixtures.ajouterReponse(premier, qcm, 0, 900);
        fixtures.ajouterReponse(fixtures.ajouterParticipant((long) id, "Deux", 0), qcm, 1, 0);

        consulter(id)
                .andExpect(jsonPath("$.connectes").value(2))
                .andExpect(jsonPath("$.repartition.total").value(2))
                .andExpect(jsonPath("$.repartition.propositions", hasSize(2)))
                .andExpect(jsonPath("$.correction").doesNotExist());

        commander(id, "questions/courante/cloture")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionOuverte").value(false))
                .andExpect(jsonPath("$.tempsRestantMs").doesNotExist())
                .andExpect(jsonPath("$.correction.bonnesPropositions[0]")
                        .value(fixtures.proposition(qcm, 0).intValue()))
                .andExpect(jsonPath("$.correction.total").value(2))
                .andExpect(jsonPath("$.correction.repartition", hasSize(2)));
        // Question close : plus d'échéance en base, comme l'attend l'UC9 pour refuser les réponses.
        assertThat(ligne(id)).containsEntry("echeance_question", null).containsEntry("position_courante", 1);

        commander(id, "questions/suivante")
                .andExpect(jsonPath("$.position").value(2))
                .andExpect(jsonPath("$.question.type").value("POINTS_A_RELIER"))
                .andExpect(jsonPath("$.question.elementsGauche", hasSize(3)))
                .andExpect(jsonPath("$.question.elementsDroite", hasSize(3)))
                .andExpect(jsonPath("$.question.propositions").doesNotExist());
        commander(id, "questions/courante/cloture")
                .andExpect(jsonPath("$.correction.paires", hasSize(3)))
                .andExpect(jsonPath("$.correction.paires[0].libelleDroite").value("HTTP"));

        commander(id, "questions/suivante")
                .andExpect(jsonPath("$.question.type").value("REPONSE_LIBRE"))
                .andExpect(jsonPath("$.question.motifs").doesNotExist());
        commander(id, "questions/courante/cloture")
                .andExpect(jsonPath("$.correction.reponsesAcceptees", hasSize(1)))
                .andExpect(jsonPath("$.correction.reponsesAcceptees[0]").value("https"));

        // Après la dernière question, « Suivant » termine la séance.
        commander(id, "questions/suivante")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CLOTUREE"))
                .andExpect(jsonPath("$.dateCloture").exists())
                .andExpect(jsonPath("$.lienAcces").doesNotExist())
                .andExpect(jsonPath("$.qrCode").doesNotExist());
        assertThat(ligne(id)).containsEntry("etat", "CLOTUREE").extractingByKey("date_cloture").isNotNull();

        for (String commande : new String[] {"questions/suivante", "questions/courante/cloture", "cloture"}) {
            commander(id, commande).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SEANCE_TERMINEE"));
        }
        consulter(id).andExpect(status().isOk()).andExpect(jsonPath("$.statut").value("CLOTUREE"));
    }

    @Test
    void les_commandes_hors_de_propos_repondent_409_avec_leur_code() throws Exception {
        fixtures.ajouterQcm(quiz, 2, "Quel protocole ?", null);
        fixtures.ajouterQcm(quiz, 4, "Et ensuite ?", null);
        int id = lancer();

        commander(id, "questions/courante/cloture").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AUCUNE_QUESTION_OUVERTE"));
        commander(id, "questions/suivante").andExpect(status().isOk());
        commander(id, "questions/suivante").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUESTION_DEJA_OUVERTE"));
    }

    @Test
    void seul_le_proprietaire_lit_et_commande_la_seance() throws Exception {
        fixtures.ajouterQcm(quiz, 2, "Quel protocole ?", null);
        int id = lancer();
        Enseignant autre = fixtures.creerEnseignant();

        mvc.perform(get("/api/sessions/{id}", id).with(user(autre.identifiant())))
                .andExpect(status().isForbidden());
        for (String commande : new String[] {"questions/suivante", "questions/courante/cloture", "cloture"}) {
            mvc.perform(post("/api/sessions/{id}/" + commande, id).with(csrf()).with(user(autre.identifiant())))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/sessions/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/sessions/{id}/cloture", id).with(user(quiz.enseignant().identifiant())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/sessions/ouverte").with(user(autre.identifiant())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Aucune séance n'est ouverte."));
        mvc.perform(get("/api/sessions/{id}", 987_654).with(user(autre.identifiant())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SEANCE_INTROUVABLE"));
        assertThat(ligne(id)).containsEntry("etat", "PREPAREE");
    }

    @Test
    void la_question_se_ferme_seule_a_l_echeance_du_serveur() throws Exception {
        fixtures.ajouterQcm(quiz, 2, "Quel protocole ?", 1);
        int id = lancer();

        commander(id, "questions/suivante").andExpect(jsonPath("$.questionOuverte").value(true));

        await().atMost(Duration.ofSeconds(8))
                .untilAsserted(() -> assertThat(ligne(id)).containsEntry("echeance_question", null));
        consulter(id).andExpect(jsonPath("$.statut").value("EN_COURS"))
                .andExpect(jsonPath("$.correction").exists());
    }

    @Test
    void le_passage_automatique_enchaine_les_questions_puis_termine_la_seance() throws Exception {
        fixtures.ajouterQcm(quiz, 2, "Quel protocole ?", 1);
        Long seconde = fixtures.ajouterQcm(quiz, 4, "Et ensuite ?", 1);
        fixtures.ajouterEcran(seconde, "BONNE_REPONSE", 0, 1);
        fixtures.activerPassageAutomatique(quiz, 0);
        int id = lancer();

        commander(id, "questions/suivante");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(ligne(id))
                .containsEntry("etat", "CLOTUREE").containsEntry("position_courante", 2));
    }

    @Test
    void au_redemarrage_une_echeance_depassee_est_close() throws Exception {
        fixtures.ajouterQcm(quiz, 2, "Quel protocole ?", null);
        int id = lancer();
        // Comme après un arrêt du serveur pendant la question : elle est ouverte en base, sans tâche programmée.
        jdbc.sql("UPDATE session_quiz SET etat = 'EN_COURS', position_courante = 1, echeance_question = :passee "
                        + "WHERE id_session = :id")
                .param("passee", LocalDateTime.now().minusMinutes(1)).param("id", id).update();

        reprendreEcheances.executer();

        await().atMost(Duration.ofSeconds(8))
                .untilAsserted(() -> assertThat(ligne(id)).containsEntry("echeance_question", null));
    }

    private int lancer() throws Exception {
        String corps = mvc.perform(post("/api/quiz/{id}/sessions", quiz.idQuiz()).with(csrf())
                        .with(user(quiz.enseignant().identifiant())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corps, "$.idSession");
    }

    private ResultActions consulter(int idSession) throws Exception {
        return mvc.perform(get("/api/sessions/{id}", idSession).with(user(quiz.enseignant().identifiant())));
    }

    private ResultActions commander(int idSession, String commande) throws Exception {
        return mvc.perform(post("/api/sessions/{id}/" + commande, idSession).with(csrf())
                .with(user(quiz.enseignant().identifiant())));
    }

    private Map<String, Object> ligne(int idSession) {
        return jdbc.sql("SELECT * FROM session_quiz WHERE id_session = :id").param("id", idSession)
                .query().singleRow();
    }
}
