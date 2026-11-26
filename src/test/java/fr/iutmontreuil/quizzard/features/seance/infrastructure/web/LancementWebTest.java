package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.jayway.jsonpath.JsonPath;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;
import fr.iutmontreuil.quizzard.features.seance.FixturesLancement;
import fr.iutmontreuil.quizzard.features.seance.FixturesLancement.Enseignant;
import fr.iutmontreuil.quizzard.features.seance.FixturesLancement.QuizDeTest;
import fr.iutmontreuil.quizzard.features.seance.application.GenerationAccesException;
import fr.iutmontreuil.quizzard.features.seance.application.port.DiffuseurSeance;
import fr.iutmontreuil.quizzard.features.seance.application.port.GenerateurQrCode;
import fr.iutmontreuil.quizzard.features.seance.infrastructure.tempsreel.CanalTempsReelException;

/**
 * Lancement d'un quiz de bout en bout (contrôleur, cas d'usage, base PostgreSQL) : réponse, écritures, tableau des
 * cas d'erreur du dossier et sécurité.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class LancementWebTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcClient jdbc;
    @MockitoSpyBean
    GenerateurQrCode generateurQrCode;
    @MockitoSpyBean
    DiffuseurSeance diffuseur;

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
    void lancer_renvoie_201_avec_la_seance_son_lien_et_son_qr_code(CapturedOutput logs) throws Exception {
        Long qcm = fixtures.ajouterQcm(quiz, 2, "Quel protocole ?", null);
        fixtures.ajouterRelier(quiz, 4);

        ResultActions reponse = lancer(quiz).andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.idQuiz").value(quiz.idQuiz().intValue()))
                .andExpect(jsonPath("$.titre").value(FixturesLancement.TITRE_QUIZ))
                .andExpect(jsonPath("$.statut").value("PREPAREE"))
                .andExpect(jsonPath("$.modeNote").value(false))
                .andExpect(jsonPath("$.nbQuestions").value(2));

        String corps = reponse.andReturn().getResponse().getContentAsString();
        int idSession = JsonPath.read(corps, "$.idSession");
        String code = JsonPath.read(corps, "$.codeAcces");
        String lien = JsonPath.read(corps, "$.lienAcces");
        reponse.andExpect(header().string("Location", "/api/sessions/" + idSession));
        assertThat(code).matches("[A-Za-z0-9_-]{22}");
        assertThat(lien).isEqualTo("http://localhost:5173/rejoindre/" + code);
        assertThat(lireQrCode(JsonPath.read(corps, "$.qrCode"))).isEqualTo(lien);
        assertThat(OffsetDateTime.parse(JsonPath.read(corps, "$.dateLancement"))).isNotNull();

        Map<String, Object> seance = jdbc.sql("SELECT * FROM session_quiz WHERE id_session = :id")
                .param("id", idSession).query().singleRow();
        assertThat(seance).containsEntry("etat", "PREPAREE").containsEntry("code_acces", code)
                .containsEntry("position_courante", 0).containsEntry("echeance_question", null)
                .containsEntry("id_version", quiz.idVersion())
                .containsEntry("identite_non_verifiee_autorisee", false);
        assertThat(jdbc.sql("SELECT verrouillee FROM question WHERE id_question = :id").param("id", qcm)
                .query(Boolean.class).single()).isTrue();

        Map<String, Object> trace = jdbc.sql(
                        "SELECT * FROM journal_evenement WHERE type_action = 'quiz_lance' AND cible = :cible")
                .param("cible", String.valueOf(idSession)).query().singleRow();
        assertThat(trace).containsEntry("acteur", quiz.enseignant().identifiant());
        assertThat(String.valueOf(trace.get("details"))).contains("nbQuestions").doesNotContain(code);
        assertThat(logs.getOut()).contains("quiz_lance").doesNotContain(code);
    }

    @Test
    void la_derniere_version_du_cours_est_presentee() throws Exception {
        fixtures.ajouterQcm(quiz, 2, "Ancienne version", null);
        Long nouvelle = fixtures.creerVersion(quiz.idCours(), 2);
        jdbc.sql("UPDATE page_inseree SET id_version = :nouvelle WHERE id_version = :ancienne")
                .param("nouvelle", nouvelle).param("ancienne", quiz.idVersion()).update();

        lancer(quiz).andExpect(status().isCreated()).andExpect(jsonPath("$.nbQuestions").value(1));

        assertThat(jdbc.sql("SELECT id_version FROM session_quiz WHERE id_quiz = :id").param("id", quiz.idQuiz())
                .query(Long.class).single()).isEqualTo(nouvelle);
    }

    @Test
    void sans_connexion_le_lancement_repond_401_et_sans_jeton_csrf_403() throws Exception {
        fixtures.ajouterQcm(quiz, 2, "Quel protocole ?", null);

        mvc.perform(post("/api/quiz/{id}/sessions", quiz.idQuiz()).with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Vous devez être connecté."));
        mvc.perform(post("/api/quiz/{id}/sessions", quiz.idQuiz())
                        .with(user(quiz.enseignant().identifiant())))
                .andExpect(status().isForbidden());

        assertThat(nombreDeSeances(quiz)).isZero();
    }

    @Test
    void un_quiz_inexistant_ou_archive_est_introuvable() throws Exception {
        lancer(quiz.enseignant(), 987_654L).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Ce quiz n'existe pas ou a été supprimé."));

        fixtures.ajouterQcm(quiz, 2, "Quel protocole ?", null);
        jdbc.sql("UPDATE quiz SET archive = TRUE WHERE id_quiz = :id").param("id", quiz.idQuiz()).update();
        lancer(quiz).andExpect(status().isNotFound());
    }

    @Test
    void le_quiz_d_un_autre_enseignant_est_refuse() throws Exception {
        fixtures.ajouterQcm(quiz, 2, "Quel protocole ?", null);

        lancer(fixtures.creerEnseignant(), quiz.idQuiz()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Action non autorisée."));

        assertThat(nombreDeSeances(quiz)).isZero();
    }

    @Test
    void une_question_incomplete_est_signalee_par_la_page_que_montre_l_editeur() throws Exception {
        // Dossier : page de quiz en (2, 0), puis une autre en (4, 0), affichée en position 4 + 1 + 1 = 6.
        fixtures.ajouterQcm(quiz, 2, "Quel protocole ?", null);
        fixtures.ajouterQcm(quiz, 4, "", null);

        lancer(quiz).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("QUIZ_INCOMPLET"))
                .andExpect(jsonPath("$.error").value(
                        "Le quiz contient une question incomplète (page 6). Complétez-la avant de lancer."));

        assertThat(nombreDeSeances(quiz)).isZero();
    }

    @Test
    void un_quiz_sans_page_de_question_est_refuse() throws Exception {
        lancer(quiz).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("QUIZ_INCOMPLET"));
    }

    @Test
    void une_seance_ouverte_bloque_un_nouveau_lancement_et_peut_etre_reprise() throws Exception {
        fixtures.ajouterQcm(quiz, 2, "Quel protocole ?", null);
        String corps = lancer(quiz).andReturn().getResponse().getContentAsString();
        int idSession = JsonPath.read(corps, "$.idSession");

        lancer(quiz).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_DEJA_OUVERTE"))
                .andExpect(jsonPath("$.error").value("Une session est déjà ouverte pour le cours « Réseaux ». "
                        + "La reprendre ou la terminer ?"));

        mvc.perform(get("/api/sessions/ouverte").with(user(quiz.enseignant().identifiant())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSession").value(idSession));
        assertThat(nombreDeSeances(quiz)).isEqualTo(1);

        mvc.perform(post("/api/sessions/{id}/cloture", idSession).with(csrf())
                .with(user(quiz.enseignant().identifiant()))).andExpect(status().isOk());
        lancer(quiz).andExpect(status().isCreated());
    }

    @Test
    void un_quiz_note_sans_annuaire_demande_l_autorisation_de_l_enseignant() throws Exception {
        QuizDeTest note = fixtures.creerQuiz(true);
        fixtures.ajouterQcm(note, 2, "Quel protocole ?", null);

        lancer(note).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ANNUAIRE_INDISPONIBLE"))
                .andExpect(jsonPath("$.error").value("L'annuaire de l'IUT est indisponible. "
                        + "Autoriser l'entrée en identité non vérifiée ?"));
        assertThat(nombreDeSeances(note)).isZero();

        mvc.perform(requete(note.enseignant(), note.idQuiz())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"autoriserIdentiteNonVerifiee\": true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.modeNote").value(true));
        assertThat(jdbc.sql("SELECT identite_non_verifiee_autorisee AND est_notee FROM session_quiz "
                        + "WHERE id_quiz = :id").param("id", note.idQuiz()).query(Boolean.class).single()).isTrue();
    }

    @Test
    void une_panne_du_qr_code_ne_cree_aucune_seance() throws Exception {
        fixtures.ajouterQcm(quiz, 2, "Quel protocole ?", null);
        doThrow(new GenerationAccesException("panne simulée", null)).when(generateurQrCode).generer(any());

        lancer(quiz).andExpect(status().isInternalServerError());

        rienNAEteEnregistre();
    }

    @Test
    void une_panne_du_canal_temps_reel_annule_la_seance_deja_ecrite() throws Exception {
        Long qcm = fixtures.ajouterQcm(quiz, 2, "Quel protocole ?", null);
        doThrow(new CanalTempsReelException()).when(diffuseur).ouvrirCanal(any());

        lancer(quiz).andExpect(status().isInternalServerError());

        rienNAEteEnregistre();
        assertThat(jdbc.sql("SELECT verrouillee FROM question WHERE id_question = :id").param("id", qcm)
                .query(Boolean.class).single()).isFalse();
    }

    @Test
    void un_identifiant_de_quiz_mal_forme_repond_400() throws Exception {
        mvc.perform(post("/api/quiz/abc/sessions").with(csrf()).with(user(quiz.enseignant().identifiant())))
                .andExpect(status().isBadRequest());
    }

    private ResultActions lancer(QuizDeTest cible) throws Exception {
        return lancer(cible.enseignant(), cible.idQuiz());
    }

    private ResultActions lancer(Enseignant enseignant, Long idQuiz) throws Exception {
        return mvc.perform(requete(enseignant, idQuiz));
    }

    private static MockHttpServletRequestBuilder requete(Enseignant enseignant, Long idQuiz) {
        return post("/api/quiz/{id}/sessions", idQuiz).with(csrf()).with(user(enseignant.identifiant()));
    }

    private int nombreDeSeances(QuizDeTest cible) {
        return jdbc.sql("SELECT count(*) FROM session_quiz WHERE id_quiz = :id").param("id", cible.idQuiz())
                .query(Integer.class).single();
    }

    private void rienNAEteEnregistre() {
        assertThat(nombreDeSeances(quiz)).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM journal_evenement WHERE type_action = 'quiz_lance' "
                        + "AND acteur = :acteur").param("acteur", quiz.enseignant().identifiant())
                .query(Integer.class).single()).isZero();
    }

    private static String lireQrCode(String base64) throws IOException, ReflectiveOperationException,
            com.google.zxing.ReaderException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        var source = new RGBLuminanceSource(image.getWidth(), image.getHeight(), pixels);
        return new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(source))).getText();
    }
}
