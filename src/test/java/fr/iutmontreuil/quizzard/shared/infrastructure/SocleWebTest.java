package fr.iutmontreuil.quizzard.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;
import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflit;
import fr.iutmontreuil.quizzard.shared.domain.ExceptionDonneesInvalides;
import fr.iutmontreuil.quizzard.shared.domain.ExceptionIntrouvable;
import fr.iutmontreuil.quizzard.shared.domain.ExceptionTypeNonSupporte;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Socle web : forme commune des erreurs, traduction des catégories en statuts, refus par défaut,
 * CORS et ligne de log par requête.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, SocleWebTest.ControleurDeTest.class})
@ExtendWith(OutputCaptureExtension.class)
class SocleWebTest {

    @Autowired
    MockMvc mvc;

    @Test
    void la_sante_est_publique_et_porte_un_identifiant_de_requete() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void une_route_protegee_sans_connexion_repond_401_au_format_commun() throws Exception {
        mvc.perform(get("/test/introuvable"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Vous devez être connecté."))
                .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    @WithMockUser
    void une_exception_introuvable_devient_404_avec_son_message() throws Exception {
        mvc.perform(get("/test/introuvable"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Le cours 42 est introuvable."))
                .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    @WithMockUser
    void chaque_categorie_a_son_statut() throws Exception {
        mvc.perform(get("/test/conflit")).andExpect(status().isConflict());
        mvc.perform(get("/test/type")).andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/test/invalide"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.titre").value("Le titre est obligatoire."));
    }

    @Test
    @WithMockUser
    void une_erreur_inattendue_renvoie_un_500_generique_sans_detail() throws Exception {
        mvc.perform(get("/test/panne"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Une erreur est survenue. Réessayez dans un instant."))
                .andExpect(content().string(not(containsString("secret-technique"))));
    }

    @Test
    @WithMockUser
    void un_dto_invalide_renvoie_422_avec_un_message_par_champ() throws Exception {
        mvc.perform(post("/test/valider").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\": \"\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.titre").exists());
    }

    @Test
    @WithMockUser
    void une_route_inexistante_renvoie_404_au_format_commun() throws Exception {
        mvc.perform(get("/test/n-existe-pas"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Ressource introuvable."));
    }

    @Test
    void le_front_react_passe_le_cors() throws Exception {
        mvc.perform(options("/test/introuvable")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void une_autre_origine_est_refusee_par_le_cors() throws Exception {
        mvc.perform(options("/test/introuvable")
                        .header("Origin", "https://site-malveillant.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void chaque_requete_produit_une_ligne_de_log_avec_statut_et_exception(CapturedOutput sortie) throws Exception {
        mvc.perform(get("/test/introuvable")).andExpect(status().isNotFound());
        assertThat(sortie.getOut())
                .contains("request_handled")
                .contains("\"status\":404")
                .contains("\"exception\":\"CoursIntrouvable\"")
                .contains("\"route\":\"/test/introuvable\"");
    }

    // ---- Contrôleur et exceptions de test : ce que fera une vraie feature ----

    static class CoursIntrouvable extends ExceptionIntrouvable {
        CoursIntrouvable() {
            super("Le cours 42 est introuvable.");
        }
    }

    static class DoublonCours extends ExceptionConflit {
        DoublonCours() {
            super("Vous avez déjà importé ce PDF.");
        }
    }

    static class FichierNonPdf extends ExceptionTypeNonSupporte {
        FichierNonPdf() {
            super("Le fichier n'est pas un PDF.");
        }
    }

    static class TitreInvalide extends ExceptionDonneesInvalides {
        TitreInvalide() {
            super("titre", "Le titre est obligatoire.");
        }
    }

    record RequeteTest(@NotBlank String titre) {
    }

    @TestConfiguration
    @RestController
    static class ControleurDeTest {

        @GetMapping("/test/introuvable")
        void introuvable() {
            throw new CoursIntrouvable();
        }

        @GetMapping("/test/conflit")
        void conflit() {
            throw new DoublonCours();
        }

        @GetMapping("/test/type")
        void type() {
            throw new FichierNonPdf();
        }

        @GetMapping("/test/invalide")
        void invalide() {
            throw new TitreInvalide();
        }

        @GetMapping("/test/panne")
        void panne() {
            throw new IllegalStateException("secret-technique : connexion au disque perdue");
        }

        @PostMapping("/test/valider")
        void valider(@Valid @RequestBody RequeteTest requete) {
        }
    }
}
