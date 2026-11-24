package fr.iutmontreuil.quizzard.features.seance.infrastructure;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * Petit client STOMP pour les tests, depuis l'origine du front : messages reçus en texte (JSON) dans des files,
 * en-tête {@code message} des trames ERROR dans {@link #erreurs}.
 */
final class ClientStompDeTest {

    private static final long ATTENTE_S = 5;

    /** En-tête {@code message} de chaque trame ERROR reçue (connexion ou abonnement refusé). */
    final BlockingQueue<String> erreurs = new LinkedBlockingQueue<>();
    private StompSession session;

    /** Ouvre la connexion ; {@code jeton} nul = sans jeton. Ne lève rien si le serveur refuse : voir erreurs. */
    static ClientStompDeTest connecter(int port, String jeton) throws InterruptedException {
        ClientStompDeTest client = new ClientStompDeTest();
        WebSocketStompClient stomp = new WebSocketStompClient(new StandardWebSocketClient());
        // Octets bruts quel que soit le type (JSON compris) : le test lit et écrit le texte lui-même.
        stomp.setMessageConverter(new ByteArrayMessageConverter() {
            @Override
            protected boolean supportsMimeType(MessageHeaders entetes) {
                return true;
            }
        });
        WebSocketHttpHeaders http = new WebSocketHttpHeaders();
        http.setOrigin("http://localhost:5173");
        StompHeaders connect = new StompHeaders();
        if (jeton != null) {
            connect.add("X-Jeton-Participant", jeton);
        }
        try {
            client.session = stomp.connectAsync("ws://localhost:" + port + "/ws", http, connect,
                    new StompSessionHandlerAdapter() {
                        @Override
                        public void handleFrame(StompHeaders entetes, Object corps) {
                            client.erreurs.add(String.valueOf(entetes.getFirst("message")));
                        }
                    }).get(ATTENTE_S, TimeUnit.SECONDS);
        } catch (Exception refus) {
            // Connexion refusée : la trame ERROR est déjà dans la file.
        }
        return client;
    }

    boolean estConnecte() {
        return session != null && session.isConnected();
    }

    /** Abonnement : chaque message arrive dans la file renvoyée, en texte. */
    BlockingQueue<String> abonner(String destination) {
        BlockingQueue<String> recus = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders entetes) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders entetes, Object corps) {
                recus.add(new String((byte[]) corps, StandardCharsets.UTF_8));
            }
        });
        return recus;
    }

    void envoyer(String destination, String json) {
        StompHeaders entetes = new StompHeaders();
        entetes.setDestination(destination);
        entetes.setContentType(MimeTypeUtils.APPLICATION_JSON);
        session.send(entetes, json.getBytes(StandardCharsets.UTF_8));
    }

    static String attendre(BlockingQueue<String> file) throws InterruptedException {
        return file.poll(ATTENTE_S, TimeUnit.SECONDS);
    }

    void fermer() {
        if (estConnecte()) {
            session.disconnect();
        }
    }
}
