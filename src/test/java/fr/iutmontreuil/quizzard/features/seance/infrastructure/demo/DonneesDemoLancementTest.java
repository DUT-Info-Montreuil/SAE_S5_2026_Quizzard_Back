package fr.iutmontreuil.quizzard.features.seance.infrastructure.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;

/** La démonstration permet de lancer et de dérouler le quiz sans connexion, et se rejoue au redémarrage. */
@SpringBootTest(properties = "quizzard.securite.ouverte=true")
@AutoConfigureMockMvc
@ActiveProfiles("demo-uc7")
@Import(TestcontainersConfiguration.class)
class DonneesDemoLancementTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    DonneesDemoLancement demo;

    @Test
    void le_quiz_de_demonstration_se_lance_et_les_donnees_se_rejouent() throws Exception {
        String corps = mvc.perform(post("/api/quiz/{id}/sessions", DonneesDemoLancement.ID_QUIZ))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nbQuestions").value(3))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/sessions/{id}/questions/suivante", JsonPath.<Integer>read(corps, "$.idSession")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question.type").value("QCM"));

        demo.run(new DefaultApplicationArguments());

        mvc.perform(post("/api/quiz/{id}/sessions", DonneesDemoLancement.ID_QUIZ)).andExpect(status().isCreated());
    }
}
