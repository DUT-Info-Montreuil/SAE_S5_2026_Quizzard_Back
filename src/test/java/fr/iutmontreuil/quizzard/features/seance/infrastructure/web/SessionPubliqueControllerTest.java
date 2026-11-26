package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;
import fr.iutmontreuil.quizzard.features.seance.FixturesSeance;
import fr.iutmontreuil.quizzard.features.seance.FixturesSeance.Seance;
import fr.iutmontreuil.quizzard.features.seance.domain.EtatSession;
import fr.iutmontreuil.quizzard.features.seance.domain.JetonParticipant;

/**
 * Routes publiques « Rejoindre un quiz » de bout en bout (contrôleur, cas d'usage, base PostgreSQL) : statuts,
 * format d'erreur, sécurité (aucune connexion ni jeton CSRF) et absence de donnée personnelle dans les logs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class SessionPubliqueControllerTest {

    private static final String MESSAGE_PSEUDONYME = "Ton pseudonyme doit faire de 2 à 20 caractères.";

    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcClient jdbc;

    FixturesSeance fixtures;
    Seance seance;

    @BeforeEach
    void preparer() {
        fixtures = new FixturesSeance(jdbc);
        seance = fixtures.creerSeance(EtatSession.PREPAREE, false);
    }

    @AfterEach
    void nettoyer() {
        fixtures.nettoyer();
    }

    private ResultActions consulter(String codeAcces) throws Exception {
        return mvc.perform(get("/api/public/sessions/{code}", codeAcces));
    }

    private ResultActions rejoindre(String codeAcces, String corps) throws Exception {
        return mvc.perform(post("/api/public/sessions/{code}/participants", codeAcces)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps));
    }

    private ResultActions rejoindreAvec(String pseudonyme) throws Exception {
        return rejoindre(seance.codeAcces(), "{\"pseudonyme\":\"" + pseudonyme + "\"}");
    }

    private int nombreDeParticipants() {
        return jdbc.sql("SELECT count(*) FROM participant WHERE id_session = :id")
                .param("id", seance.idSession()).query(Integer.class).single();
    }

    // ---- GET /api/public/sessions/{codeAcces} ----

    @Test
    void ouvrir_le_lien_renvoie_titres_mode_note_et_etat_sans_compte_ni_csrf() throws Exception {
        consulter(seance.codeAcces())
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.titreCours").value(FixturesSeance.TITRE_COURS))
                .andExpect(jsonPath("$.titreQuiz").value(FixturesSeance.TITRE_QUIZ))
                .andExpect(jsonPath("$.estNotee").value(false))
                .andExpect(jsonPath("$.etat").value("PREPAREE"));
    }

    @Test
    void un_quiz_note_est_consultable_et_signale_son_mode() throws Exception {
        Seance notee = fixtures.creerSeance(EtatSession.EN_COURS, true);

        consulter(notee.codeAcces())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estNotee").value(true))
                .andExpect(jsonPath("$.etat").value("EN_COURS"));
    }

    @Test
    void un_code_d_acces_inconnu_donne_404_avec_le_message_pour_l_etudiant() throws Exception {
        consulter("code-qui-n-existe-pas")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Ce quiz est introuvable."))
                .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    @Test
    void une_seance_cloturee_donne_409() throws Exception {
        Seance cloturee = fixtures.creerSeance(EtatSession.CLOTUREE, false);

        consulter(cloturee.codeAcces())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ce quiz est terminé."));
    }

    // ---- POST /api/public/sessions/{codeAcces}/participants ----

    @Test
    void rejoindre_donne_201_avec_le_jeton_et_n_enregistre_que_son_empreinte() throws Exception {
        String corps = rejoindreAvec("  Alice  ")
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.idParticipant").isNumber())
                .andExpect(jsonPath("$.jeton").value(matchesPattern("[A-Za-z0-9_-]{43}")))
                .andExpect(jsonPath("$.pseudonyme").value("Alice"))
                .andReturn().getResponse().getContentAsString();

        String jeton = JsonPath.read(corps, "$.jeton");
        Number idParticipant = JsonPath.read(corps, "$.idParticipant");
        Map<String, Object> ligne = jdbc.sql("""
                        SELECT pseudonyme, jeton_reconnexion, score_total, identite_verifiee, id_session
                        FROM participant WHERE id_participant = :id
                        """)
                .param("id", idParticipant.longValue()).query().singleRow();
        assertThat(ligne.get("pseudonyme")).isEqualTo("Alice");
        assertThat(ligne.get("jeton_reconnexion")).isEqualTo(JetonParticipant.empreinte(jeton));
        assertThat(ligne.get("jeton_reconnexion")).isNotEqualTo(jeton);
        assertThat(ligne.get("score_total")).isEqualTo(0);
        assertThat(ligne.get("identite_verifiee")).isEqualTo(false);
        assertThat(ligne.get("id_session")).isEqualTo(seance.idSession());
    }

    @Test
    void on_peut_rejoindre_une_seance_deja_en_cours() throws Exception {
        Seance enCours = fixtures.creerSeance(EtatSession.EN_COURS, false);

        rejoindre(enCours.codeAcces(), "{\"pseudonyme\":\"Retardataire\"}").andExpect(status().isCreated());
    }

    @Test
    void deux_etudiants_ont_des_jetons_differents() throws Exception {
        String premier = JsonPath.read(rejoindreAvec("Alice").andReturn().getResponse().getContentAsString(), "$.jeton");
        String second = JsonPath.read(rejoindreAvec("Bob").andReturn().getResponse().getContentAsString(), "$.jeton");

        assertThat(premier).isNotEqualTo(second);
        assertThat(nombreDeParticipants()).isEqualTo(2);
    }

    @Test
    void un_code_d_acces_inconnu_donne_404() throws Exception {
        rejoindre("code-qui-n-existe-pas", "{\"pseudonyme\":\"Alice\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Ce quiz est introuvable."));
    }

    @Test
    void une_seance_cloturee_refuse_l_entree_avec_409() throws Exception {
        Seance cloturee = fixtures.creerSeance(EtatSession.CLOTUREE, false);

        rejoindre(cloturee.codeAcces(), "{\"pseudonyme\":\"Alice\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ce quiz est terminé."));
    }

    @Test
    void un_quiz_note_refuse_l_entree_sans_compte_avec_409() throws Exception {
        Seance notee = fixtures.creerSeance(EtatSession.PREPAREE, true);

        rejoindre(notee.codeAcces(), "{\"pseudonyme\":\"Alice\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ce quiz est noté : connecte-toi avec ton compte IUT."));

        assertThat(jdbc.sql("SELECT count(*) FROM participant WHERE id_session = :id")
                .param("id", notee.idSession()).query(Integer.class).single()).isZero();
    }

    @Test
    void un_pseudonyme_deja_pris_donne_409_avec_une_suggestion() throws Exception {
        rejoindreAvec("Alice").andExpect(status().isCreated());

        rejoindreAvec("Alice")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ce pseudonyme est déjà pris. Essaie celui qui t'est proposé."))
                .andExpect(jsonPath("$.suggestion").value("Alice2"))
                .andExpect(jsonPath("$.request_id").isNotEmpty());

        assertThat(nombreDeParticipants()).isEqualTo(1);
    }

    @Test
    void la_suggestion_est_un_pseudonyme_libre_de_la_seance() throws Exception {
        rejoindreAvec("Alice").andExpect(status().isCreated());
        rejoindreAvec("Alice2").andExpect(status().isCreated());

        rejoindreAvec("Alice")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.suggestion").value("Alice3"));
    }

    @Test
    void la_suggestion_est_acceptee_par_la_seance() throws Exception {
        rejoindreAvec("Alice").andExpect(status().isCreated());

        rejoindreAvec("Alice2").andExpect(status().isCreated()).andExpect(jsonPath("$.pseudonyme").value("Alice2"));
    }

    @Test
    void le_meme_pseudonyme_est_libre_dans_une_autre_seance() throws Exception {
        Seance autre = fixtures.creerSeance(EtatSession.PREPAREE, false);
        rejoindreAvec("Alice").andExpect(status().isCreated());

        rejoindre(autre.codeAcces(), "{\"pseudonyme\":\"Alice\"}").andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"pseudonyme\":null}",
            "{\"pseudonyme\":\"\"}",
            "{\"pseudonyme\":\"   \"}",
            "{\"pseudonyme\":\"A\"}",
            "{\"pseudonyme\":\"AAAAAAAAAAAAAAAAAAAAA\"}"})
    void un_pseudonyme_absent_vide_ou_hors_taille_donne_400_requete_invalide_avec_le_champ_fautif(String corps) throws Exception {
        rejoindre(seance.codeAcces(), corps)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"))
                .andExpect(jsonPath("$.error").value("Certaines valeurs sont invalides."))
                .andExpect(jsonPath("$.champs.pseudonyme").value(MESSAGE_PSEUDONYME));

        assertThat(nombreDeParticipants()).isZero();
    }

    @Test
    void un_pseudonyme_trop_court_une_fois_nettoye_est_refuse_par_le_domaine_en_dernier_rempart() throws Exception {
        // 5 caractères pour le contrôleur, mais un seul après nettoyage des espaces.
        rejoindreAvec("  A  ")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.pseudonyme").value(MESSAGE_PSEUDONYME));

        assertThat(nombreDeParticipants()).isZero();
    }

    @Test
    void un_pseudonyme_avec_un_caractere_de_controle_donne_422() throws Exception {
        rejoindre(seance.codeAcces(), "{\"pseudonyme\":\"Al\\u0000ice\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.champs.pseudonyme").value(MESSAGE_PSEUDONYME));
    }

    @Test
    void un_corps_illisible_donne_400_au_format_commun() throws Exception {
        rejoindre(seance.codeAcces(), "ceci n'est pas du json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La requête est mal formée."))
                .andExpect(jsonPath("$.request_id").isNotEmpty());
    }

    // ---- Sécurité ----

    @Test
    void seules_les_routes_publiques_sont_ouvertes_sans_connexion() throws Exception {
        mvc.perform(get("/api/sessions/{id}", seance.idSession())).andExpect(status().isUnauthorized());
    }

    @Test
    void le_front_react_peut_rejoindre_depuis_son_origine_avec_le_jeton_de_participant() throws Exception {
        mvc.perform(options("/api/public/sessions/{code}/participants", seance.codeAcces())
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-jeton-participant"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    // ---- Journal : aucune donnée personnelle ----

    @Test
    void l_entree_est_tracee_sans_pseudonyme_ni_jeton(CapturedOutput sortie) throws Exception {
        String corps = rejoindreAvec("PseudoDansLesLogs").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String jeton = JsonPath.read(corps, "$.jeton");

        assertThat(sortie.getAll())
                .contains("participant_entre")
                .doesNotContain("PseudoDansLesLogs")
                .doesNotContain(jeton)
                .doesNotContain(JetonParticipant.empreinte(jeton));
    }

    @Test
    void les_refus_sont_traces_sans_pseudonyme_ni_suggestion(CapturedOutput sortie) throws Exception {
        rejoindreAvec("PseudoRefuse").andExpect(status().isCreated());
        rejoindreAvec("PseudoRefuse").andExpect(status().isConflict());
        Seance cloturee = fixtures.creerSeance(EtatSession.CLOTUREE, false);
        rejoindre(cloturee.codeAcces(), "{\"pseudonyme\":\"PseudoRefuse\"}").andExpect(status().isConflict());
        Seance notee = fixtures.creerSeance(EtatSession.PREPAREE, true);
        rejoindre(notee.codeAcces(), "{\"pseudonyme\":\"PseudoRefuse\"}").andExpect(status().isConflict());

        assertThat(sortie.getAll())
                .contains("entree_refusee_pseudonyme_pris")
                .contains("entree_refusee_session_close")
                .contains("entree_refusee_quiz_note")
                .doesNotContain("PseudoRefuse")
                .doesNotContain("PseudoRefuse2");
    }
}
