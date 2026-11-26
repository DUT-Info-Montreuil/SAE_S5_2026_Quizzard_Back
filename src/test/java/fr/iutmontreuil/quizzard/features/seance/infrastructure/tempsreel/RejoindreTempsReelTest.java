package fr.iutmontreuil.quizzard.features.seance.infrastructure.tempsreel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;
import fr.iutmontreuil.quizzard.features.seance.FixturesSeance;
import fr.iutmontreuil.quizzard.features.seance.FixturesSeance.Seance;
import fr.iutmontreuil.quizzard.features.seance.domain.EtatSession;

/**
 * Compteur de connectés de l'écran de l'enseignant, de bout en bout : un vrai client STOMP s'abonne à
 * {@code /topic/sessions/{codeAcces}/connectes}, puis des étudiants rejoignent par l'API REST.
 * <p>
 * L'écran de l'enseignant n'a pas encore de connexion (feature {@code compte}) :
 * ce test utilise l'interrupteur de développement {@code quizzard.securite.ouverte}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "quizzard.securite.ouverte=true")
@Import(TestcontainersConfiguration.class)
class RejoindreTempsReelTest {

    private static final String CHAMP_SONDE = "sonde";

    @LocalServerPort
    int port;
    @Autowired
    JdbcClient jdbc;
    @Autowired
    SimpMessagingTemplate messagerie;

    FixturesSeance fixtures;
    Seance seance;
    WebSocketStompClient clientStomp;
    StompSession sessionStomp;
    RestClient rest;
    final BlockingQueue<Map<String, Object>> messages = new LinkedBlockingQueue<>();
    final BlockingQueue<Map<String, Object>> sondes = new LinkedBlockingQueue<>();

    @BeforeEach
    void preparer() throws Exception {
        fixtures = new FixturesSeance(jdbc);
        seance = fixtures.creerSeance(EtatSession.PREPAREE, false);
        rest = RestClient.create("http://localhost:" + port);

        clientStomp = new WebSocketStompClient(new StandardWebSocketClient());
        clientStomp.setMessageConverter(new JacksonJsonMessageConverter());
        WebSocketHttpHeaders entetes = new WebSocketHttpHeaders();
        entetes.setOrigin("http://localhost:5173");
        sessionStomp = clientStomp.connectAsync("ws://localhost:" + port + "/ws", entetes,
                new StompSessionHandlerAdapter() {
                }).get(10, TimeUnit.SECONDS);

        String destination = destinationConnectes();
        sessionStomp.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders entetes) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders entetes, Object corps) {
                Map<String, Object> message = (Map<String, Object>) corps;
                (message.containsKey(CHAMP_SONDE) ? sondes : messages).add(message);
            }
        });
        attendreQueLAbonnementSoitActif(destination);
    }

    @AfterEach
    void nettoyer() {
        if (sessionStomp != null && sessionStomp.isConnected()) {
            sessionStomp.disconnect();
        }
        clientStomp.stop();
        fixtures.nettoyer();
    }

    private String destinationConnectes() {
        return "/topic/sessions/" + seance.codeAcces() + "/connectes";
    }

    /**
     * Le serveur enregistre l'abonnement un peu après l'envoi de la trame SUBSCRIBE. Des messages « sonde » (rangés à
     * part, ignorés par les tests) sont envoyés sur le même canal jusqu'à ce que le client en reçoive un :
     * l'abonnement est alors actif.
     */
    private void attendreQueLAbonnementSoitActif(String destination) throws InterruptedException {
        await().atMost(Duration.ofSeconds(10)).until(() -> {
            messagerie.convertAndSend(destination, (Object) Map.of(CHAMP_SONDE, true));
            return sondes.poll(200, TimeUnit.MILLISECONDS) != null;
        });
        // Les sondes encore en route sont écartées pour ne pas fausser les assertions.
        Thread.sleep(200);
        sondes.clear();
    }

    private void rejoindre(String pseudonyme) {
        rest.post().uri("/api/public/sessions/{code}/participants", seance.codeAcces())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("pseudonyme", pseudonyme))
                .retrieve().toBodilessEntity();
    }

    @Test
    void chaque_entree_envoie_le_nombre_de_connectes_a_l_ecran_de_l_enseignant() throws Exception {
        rejoindre("Alice");
        assertThat(messages.poll(10, TimeUnit.SECONDS)).containsEntry("nombre", 1);

        rejoindre("Bob");
        assertThat(messages.poll(10, TimeUnit.SECONDS)).containsEntry("nombre", 2);
    }

    @Test
    void une_entree_refusee_n_annonce_aucun_participant() throws Exception {
        rejoindre("Alice");
        assertThat(messages.poll(10, TimeUnit.SECONDS)).containsEntry("nombre", 1);

        assertThatThrownBy(() -> rejoindre("Alice")).isInstanceOf(HttpClientErrorException.Conflict.class);

        assertThat(messages.poll(1, TimeUnit.SECONDS)).as("aucun nouveau message").isNull();
    }
}
