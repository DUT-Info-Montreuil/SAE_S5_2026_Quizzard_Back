package fr.iutmontreuil.quizzard.features.seance.infrastructure;

import static fr.iutmontreuil.quizzard.features.seance.infrastructure.ClientStompDeTest.attendre;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;

/**
 * Canal temps réel de l'étudiant (sécurité fermée) : connexion par jeton, abonnements limités à ses canaux, et
 * réponse envoyée par STOMP comme le décrit le dossier de conception (accusé et erreurs dans ses files).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Sql("/seance/seance-en-cours.sql")
class TempsReelSeanceTest {

    private static final String JETON = "jeton-de-test-uc9";
    private static final String REPONSES = "/app/sessions/1000/reponses";

    @LocalServerPort
    int port;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    Clock horloge;

    private ClientStompDeTest client;

    @BeforeEach
    void ouvrirLaQuestion() {
        jdbc.sql("UPDATE session_quiz SET position_courante = 1, echeance_question = :e WHERE id_session = 1000")
                .param("e", LocalDateTime.now(horloge).plusSeconds(15)).update();
    }

    @AfterEach
    void fermer() {
        if (client != null) {
            client.fermer();
        }
    }

    @Test
    void un_jeton_inconnu_est_refuse_a_la_connexion_avec_jeton_invalide() throws Exception {
        client = ClientStompDeTest.connecter(port, "jeton-inconnu");
        assertThat(attendre(client.erreurs)).isEqualTo("JETON_INVALIDE");
        assertThat(client.estConnecte()).isFalse();
    }

    @Test
    void sans_jeton_la_connexion_est_refusee_hors_developpement() throws Exception {
        client = ClientStompDeTest.connecter(port, null);
        assertThat(attendre(client.erreurs)).isEqualTo("JETON_INVALIDE");
    }

    @Test
    void un_etudiant_ne_peut_pas_s_abonner_a_la_repartition_de_l_enseignant() throws Exception {
        client = ClientStompDeTest.connecter(port, JETON);
        assertThat(client.estConnecte()).isTrue();
        client.abonner("/topic/sessions/QUIZ-UC9/repartition");
        assertThat(attendre(client.erreurs)).isEqualTo("ABONNEMENT_REFUSE");
    }

    @Test
    void une_reponse_par_stomp_recoit_un_accuse_avec_son_request_id() throws Exception {
        client = ClientStompDeTest.connecter(port, JETON);
        BlockingQueue<String> accuses = client.abonner("/user/queue/reponses");
        BlockingQueue<String> question = client.abonner("/topic/sessions/QUIZ-UC9/question");
        client.envoyer(REPONSES, "{\"requestId\": \"r-1\", \"idQuestion\": 1000, \"idPropositions\": [1000]}");

        String accuse = attendre(accuses);
        assertThat(accuse).contains("\"requestId\":\"r-1\"").contains("\"idQuestion\":1000")
                .contains("horodatageServeur").doesNotContain("points");
        assertThat(question).isEmpty();
        assertThat(jdbc.sql("SELECT score_total FROM participant WHERE id_participant = 1000")
                .query(Integer.class).single()).isPositive();
    }

    @Test
    void une_seconde_reponse_par_stomp_recoit_deja_repondu_dans_sa_file_d_erreurs() throws Exception {
        client = ClientStompDeTest.connecter(port, JETON);
        BlockingQueue<String> accuses = client.abonner("/user/queue/reponses");
        BlockingQueue<String> erreurs = client.abonner("/user/queue/erreurs");
        client.envoyer(REPONSES, "{\"requestId\": \"r-1\", \"idQuestion\": 1000, \"idPropositions\": [1000]}");
        assertThat(attendre(accuses)).isNotNull();

        client.envoyer(REPONSES, "{\"requestId\": \"r-2\", \"idQuestion\": 1000, \"idPropositions\": [1001]}");
        assertThat(attendre(erreurs)).contains("\"requestId\":\"r-2\"").contains("\"code\":\"DEJA_REPONDU\"")
                .contains("\"statut\":409").contains("Tu as déjà répondu.");
    }

    @Test
    void un_message_vers_une_autre_seance_est_refuse_avec_jeton_invalide() throws Exception {
        client = ClientStompDeTest.connecter(port, JETON);
        BlockingQueue<String> erreurs = client.abonner("/user/queue/erreurs");
        client.envoyer("/app/sessions/999/reponses", "{\"requestId\": \"r-3\", \"idQuestion\": 1000, "
                + "\"idPropositions\": [1000]}");
        assertThat(attendre(erreurs)).contains("\"code\":\"JETON_INVALIDE\"").contains("\"statut\":401");
    }

    @Test
    void un_message_sans_question_est_refuse_avec_requete_invalide() throws Exception {
        client = ClientStompDeTest.connecter(port, JETON);
        BlockingQueue<String> erreurs = client.abonner("/user/queue/erreurs");
        client.envoyer(REPONSES, "{\"requestId\": \"r-4\", \"idPropositions\": [1000]}");
        assertThat(attendre(erreurs)).contains("\"code\":\"REQUETE_INVALIDE\"").contains("\"statut\":400")
                .contains("\"requestId\":\"r-4\"");
    }
}
