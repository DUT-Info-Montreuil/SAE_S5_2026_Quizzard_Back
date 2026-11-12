package fr.iutmontreuil.quizzard.shared.infrastructure;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;

/** Interrupteur de développement SECURITE_OUVERTE : les routes répondent sans connexion ni jeton CSRF. */
@SpringBootTest(properties = "quizzard.securite.ouverte=true")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, SocleWebTest.ControleurDeTest.class})
class SecuriteOuverteTest {

    @Autowired
    MockMvc mvc;

    @Test
    void une_route_repond_sans_connexion() throws Exception {
        mvc.perform(get("/test/introuvable"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Le cours 42 est introuvable."));
    }

    @Test
    void un_post_passe_sans_jeton_csrf() throws Exception {
        mvc.perform(post("/test/valider").contentType(MediaType.APPLICATION_JSON).content("{\"titre\": \"Réseaux\"}"))
                .andExpect(status().isOk());
    }
}
