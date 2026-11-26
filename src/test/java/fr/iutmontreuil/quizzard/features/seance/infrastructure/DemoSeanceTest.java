package fr.iutmontreuil.quizzard.features.seance.infrastructure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;

/**
 * Profil de démonstration : séance DEMO prête au démarrage, avancée par les routes qui remplacent l'UC7. Les tests
 * partagent la séance : le premier vérifie l'état de départ, avant que les suivants la fassent avancer.
 */
@SpringBootTest(properties = "quizzard.securite.ouverte=true")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@DirtiesContext
@Import(TestcontainersConfiguration.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DemoSeanceTest {

    private static final String JETON = "jeton-demo";

    @Autowired
    MockMvc mvc;

    @Test
    @Order(1)
    void la_seance_de_demo_est_prete_au_demarrage_avec_le_qcm_ouvert() throws Exception {
        mvc.perform(get("/api/public/sessions/DEMO/etat").header("X-Jeton-Participant", JETON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question.idQuestion").value(900000))
                .andExpect(jsonPath("$.question.type").value("QCM"))
                .andExpect(jsonPath("$.question.dureeSec").value(60));
    }

    @Test
    @Order(3)
    void les_routes_de_demo_font_avancer_la_seance_jusqu_a_la_cloture() throws Exception {
        mvc.perform(post("/api/demo/sessions/DEMO/questions/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("POINTS_A_RELIER"))
                .andExpect(jsonPath("$.elementsGauche.length()").value(3));
        mvc.perform(post("/api/public/sessions/DEMO/reponses").header("X-Jeton-Participant", JETON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idQuestion\": 900001, \"liens\": [{\"gauche\": 900000, \"droite\": 900000}]}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/demo/sessions/DEMO/questions/3"))
                .andExpect(jsonPath("$.type").value("REPONSE_LIBRE"));
        mvc.perform(post("/api/demo/sessions/DEMO/cloture")).andExpect(status().isNoContent());
        mvc.perform(post("/api/public/sessions/DEMO/reponses").header("X-Jeton-Participant", JETON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idQuestion\": 900002, \"texteLibre\": \"https\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUESTION_CLOSE"));
    }

    @Test
    @Order(2)
    void une_position_sans_question_renvoie_404() throws Exception {
        mvc.perform(post("/api/demo/sessions/DEMO/questions/9")).andExpect(status().isNotFound());
    }
}
