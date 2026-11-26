package fr.iutmontreuil.quizzard.features.seance.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;
import fr.iutmontreuil.quizzard.features.seance.FixturesLancement;
import fr.iutmontreuil.quizzard.features.seance.FixturesLancement.QuizDeTest;

/**
 * Événements du dossier reçus sur le canal temps réel de la séance, par un vrai client STOMP. La connexion de
 * l'enseignant au canal viendra avec la feature {@code compte} : le test ouvre la sécurité, comme en développement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "quizzard.securite.ouverte=true")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DeroulementTempsReelTest {

    @LocalServerPort
    int port;
    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcClient jdbc;
    @Autowired
    SimpMessagingTemplate messagerie;

    FixturesLancement fixtures;
    QuizDeTest quiz;
    ClientStompDeTest client;

    @BeforeEach
    void preparer() throws InterruptedException {
        fixtures = new FixturesLancement(jdbc);
        quiz = fixtures.creerQuiz(false);
        client = ClientStompDeTest.connecter(port, null);
        assertThat(client.estConnecte()).isTrue();
    }

    @AfterEach
    void nettoyer() {
        client.fermer();
        fixtures.nettoyer();
    }

    @Test
    void la_question_part_sans_reponse_attendue_et_la_correction_seulement_a_la_cloture() throws Exception {
        Long qcm = fixtures.ajouterQcm(quiz, 2, "Quel protocole ?", null);
        fixtures.ajouterEcran(qcm, "BONNE_REPONSE", 0, 0);
        String corps = lancer();
        int id = JsonPath.read(corps, "$.idSession");
        String canal = "/topic/sessions/" + JsonPath.<String>read(corps, "$.codeAcces") + "/";
        BlockingQueue<String> etats = abonner(canal + "etat");
        BlockingQueue<String> questions = abonner(canal + "question");
        BlockingQueue<String> corrections = abonner(canal + "correction");
        BlockingQueue<String> ecrans = abonner(canal + "ecran");

        commander(id, "questions/suivante");

        String etat = ClientStompDeTest.attendre(etats);
        assertThat((String) JsonPath.read(etat, "$.etat")).isEqualTo("EN_COURS");
        assertThat((int) JsonPath.read(etat, "$.position")).isEqualTo(1);
        String question = ClientStompDeTest.attendre(questions);
        assertThat((String) JsonPath.read(question, "$.enonce")).isEqualTo("Quel protocole ?");
        assertThat((String) JsonPath.read(question, "$.echeance")).matches(".*T.*([+-]\\d{2}:\\d{2}|Z)");
        assertThat((int) JsonPath.read(question, "$.propositions.length()")).isEqualTo(2);
        assertThat(question).doesNotContain("estCorrecte").doesNotContain("bonne");
        assertThat(corrections.poll(300, TimeUnit.MILLISECONDS)).as("rien avant la clôture").isNull();

        commander(id, "questions/courante/cloture");

        String correction = ClientStompDeTest.attendre(corrections);
        assertThat((int) JsonPath.read(correction, "$.bonnesPropositions[0]"))
                .isEqualTo(fixtures.proposition(qcm, 0).intValue());
        assertThat((String) JsonPath.read(ClientStompDeTest.attendre(ecrans), "$.type"))
                .isEqualTo("BONNE_REPONSE");
        assertThat((Object) JsonPath.read(ClientStompDeTest.attendre(etats), "$.echeance")).isNull();

        commander(id, "cloture");

        assertThat((String) JsonPath.read(ClientStompDeTest.attendre(etats), "$.etat")).isEqualTo("CLOTUREE");
    }

    @Test
    void a_l_echeance_le_serveur_envoie_la_correction_puis_les_ecrans_a_leur_heure() throws Exception {
        Long qcm = fixtures.ajouterQcm(quiz, 2, "Quel protocole ?", 1);
        fixtures.ajouterEcran(qcm, "CLASSEMENT", 0, 1);
        String corps = lancer();
        int id = JsonPath.read(corps, "$.idSession");
        String canal = "/topic/sessions/" + JsonPath.<String>read(corps, "$.codeAcces") + "/";
        BlockingQueue<String> corrections = abonner(canal + "correction");
        BlockingQueue<String> ecrans = abonner(canal + "ecran");

        commander(id, "questions/suivante");

        assertThat(ClientStompDeTest.attendre(corrections)).as("correction sans commande de clôture").isNotNull();
        assertThat((String) JsonPath.read(ClientStompDeTest.attendre(ecrans), "$.type")).isEqualTo("CLASSEMENT");
    }

    /** Abonne puis attend que l'abonnement soit enregistré par le serveur, grâce à des messages sonde. */
    private BlockingQueue<String> abonner(String destination) throws InterruptedException {
        BlockingQueue<String> recus = client.abonner(destination);
        String sonde = null;
        for (int essai = 0; essai < 100 && sonde == null; essai++) {
            messagerie.convertAndSend(destination, "sonde");
            sonde = recus.poll(50, TimeUnit.MILLISECONDS);
        }
        assertThat(sonde).as("abonnement à " + destination.substring(destination.lastIndexOf('/'))).isNotNull();
        while (recus.poll(200, TimeUnit.MILLISECONDS) != null) {
            // Les sondes restantes sont retirées de la file.
        }
        return recus;
    }

    private String lancer() throws Exception {
        return mvc.perform(post("/api/quiz/{id}/sessions", quiz.idQuiz())
                        .with(user(quiz.enseignant().identifiant())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private void commander(int idSession, String commande) throws Exception {
        mvc.perform(post("/api/sessions/{id}/" + commande, idSession).with(user(quiz.enseignant().identifiant())))
                .andExpect(status().isOk());
    }
}
