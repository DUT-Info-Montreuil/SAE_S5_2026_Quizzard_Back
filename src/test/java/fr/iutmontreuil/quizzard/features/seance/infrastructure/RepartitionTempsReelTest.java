package fr.iutmontreuil.quizzard.features.seance.infrastructure;

import static fr.iutmontreuil.quizzard.features.seance.infrastructure.ClientStompDeTest.attendre;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;

/**
 * Répartition en direct vers l'écran de l'enseignant, après une réponse validée. La connexion enseignant n'existe
 * pas encore : l'écran se connecte sans jeton, ce que permet le mode développement ({@code SECURITE_OUVERTE}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "quizzard.securite.ouverte=true")
@Import(TestcontainersConfiguration.class)
@Sql("/seance/seance-en-cours.sql")
class RepartitionTempsReelTest {

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    Clock horloge;

    @Test
    void l_ecran_de_l_enseignant_recoit_la_repartition_apres_une_reponse() throws Exception {
        jdbc.sql("UPDATE session_quiz SET position_courante = 1, echeance_question = :e WHERE id_session = 1000")
                .param("e", LocalDateTime.now(horloge).plusSeconds(15)).update();
        ClientStompDeTest ecran = ClientStompDeTest.connecter(port, null);
        try {
            BlockingQueue<String> repartition = ecran.abonner("/topic/sessions/QUIZ-UC9/repartition");
            Thread.sleep(200);

            HttpResponse<String> reponse = HttpClient.newHttpClient().send(HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:" + port + "/api/public/sessions/QUIZ-UC9/reponses"))
                            .header("Content-Type", "application/json")
                            .header("X-Jeton-Participant", "jeton-de-test-uc9")
                            .POST(HttpRequest.BodyPublishers.ofString("{\"idQuestion\":1000,\"idPropositions\":[1000]}"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(reponse.statusCode()).isEqualTo(201);

            assertThat(attendre(repartition))
                    .contains("\"idQuestion\":1000")
                    .contains("\"total\":1")
                    .contains("{\"idProposition\":1000,\"nombre\":1}");
        } finally {
            ecran.fermer();
        }
    }
}
