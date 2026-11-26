package fr.iutmontreuil.quizzard.features.seance.infrastructure.persistance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;
import fr.iutmontreuil.quizzard.features.seance.FixturesSeance;
import fr.iutmontreuil.quizzard.features.seance.FixturesSeance.Seance;
import fr.iutmontreuil.quizzard.features.seance.application.dto.TitresQuiz;
import fr.iutmontreuil.quizzard.features.seance.application.port.InformationsQuiz;
import fr.iutmontreuil.quizzard.features.seance.application.port.EntreeParticipantRepository;
import fr.iutmontreuil.quizzard.features.seance.application.port.SessionQuizRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.EtatSession;
import fr.iutmontreuil.quizzard.features.seance.domain.JetonParticipant;
import fr.iutmontreuil.quizzard.features.seance.domain.ParticipantEntrant;
import fr.iutmontreuil.quizzard.features.seance.domain.Pseudonyme;
import fr.iutmontreuil.quizzard.features.seance.domain.PseudonymeDejaPrisException;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionQuiz;

/**
 * Dépôts de la feature {@code seance} sur un vrai PostgreSQL (Testcontainers) : les migrations Flyway sont appliquées
 * sur une base vierge, puis Hibernate valide les entités ({@code ddl-auto=validate}).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class SeancePersistanceTest {

    @Autowired
    SessionQuizRepository sessions;
    @Autowired
    EntreeParticipantRepository participants;
    @Autowired
    InformationsQuiz informationsQuiz;
    @Autowired
    JdbcClient jdbc;

    FixturesSeance fixtures;
    Seance seance;

    @BeforeEach
    void preparer() {
        fixtures = new FixturesSeance(jdbc);
        seance = fixtures.creerSeance(EtatSession.EN_COURS, false);
    }

    @AfterEach
    void nettoyer() {
        fixtures.nettoyer();
    }

    private static ParticipantEntrant nouveau(Long idSession, String pseudonyme) {
        return ParticipantEntrant.rejoindre(idSession, Pseudonyme.de(pseudonyme),
                JetonParticipant.empreinte(UUID.randomUUID().toString()), LocalDateTime.now());
    }

    // ---- Séances ----

    @Test
    void une_seance_est_retrouvee_par_son_code_d_acces() {
        SessionQuiz session = sessions.trouverParCodeAcces(seance.codeAcces()).orElseThrow();

        assertThat(session.idSession()).isEqualTo(seance.idSession());
        assertThat(session.codeAcces()).isEqualTo(seance.codeAcces());
        assertThat(session.etat()).isEqualTo(EtatSession.EN_COURS);
        assertThat(session.estNotee()).isFalse();
        assertThat(session.positionCourante()).isZero();
    }

    @Test
    void le_mode_note_et_l_etat_cloture_sont_lus_depuis_la_base() {
        Seance cloturee = fixtures.creerSeance(EtatSession.CLOTUREE, true);

        SessionQuiz session = sessions.trouverParCodeAcces(cloturee.codeAcces()).orElseThrow();

        assertThat(session.etat()).isEqualTo(EtatSession.CLOTUREE);
        assertThat(session.estNotee()).isTrue();
    }

    @Test
    void un_code_d_acces_inconnu_ne_donne_rien() {
        assertThat(sessions.trouverParCodeAcces("code-qui-n-existe-pas")).isEmpty();
    }

    @Test
    void les_titres_du_quiz_et_du_cours_sont_lus() {
        TitresQuiz titres = informationsQuiz.titresDeLaSession(seance.idSession());

        assertThat(titres.titreCours()).isEqualTo(FixturesSeance.TITRE_COURS);
        assertThat(titres.titreQuiz()).isEqualTo(FixturesSeance.TITRE_QUIZ);
    }

    // ---- Participants ----

    @Test
    void un_participant_enregistre_recoit_un_identifiant_et_n_est_stocke_que_par_l_empreinte_de_son_jeton() {
        ParticipantEntrant participant = nouveau(seance.idSession(), "Alice");

        ParticipantEntrant enregistre = participants.enregistrer(participant);

        assertThat(enregistre.idParticipant()).isNotNull();
        Map<String, Object> ligne = jdbc.sql("""
                        SELECT pseudonyme, jeton_reconnexion, score_total, identite_verifiee, identifiant_ldap,
                               id_session
                        FROM participant WHERE id_participant = :id
                        """)
                .param("id", enregistre.idParticipant())
                .query().singleRow();
        assertThat(ligne.get("pseudonyme")).isEqualTo("Alice");
        assertThat(ligne.get("jeton_reconnexion")).isEqualTo(participant.empreinteJeton());
        assertThat(((String) ligne.get("jeton_reconnexion"))).hasSize(64);
        assertThat(ligne.get("score_total")).isEqualTo(0);
        assertThat(ligne.get("identite_verifiee")).isEqualTo(false);
        assertThat(ligne.get("identifiant_ldap")).isNull();
        assertThat(ligne.get("id_session")).isEqualTo(seance.idSession());
    }

    @Test
    void un_pseudonyme_existant_est_detecte_a_l_identique_dans_la_seance() {
        participants.enregistrer(nouveau(seance.idSession(), "Alice"));

        assertThat(participants.existePseudonyme(seance.idSession(), Pseudonyme.de("Alice"))).isTrue();
        assertThat(participants.existePseudonyme(seance.idSession(), Pseudonyme.de("alice"))).isFalse();
        assertThat(participants.existePseudonyme(seance.idSession(), Pseudonyme.de("Bob"))).isFalse();
    }

    @Test
    void le_meme_pseudonyme_est_libre_dans_une_autre_seance() {
        Seance autre = fixtures.creerSeance(EtatSession.PREPAREE, false);
        participants.enregistrer(nouveau(seance.idSession(), "Alice"));

        assertThat(participants.existePseudonyme(autre.idSession(), Pseudonyme.de("Alice"))).isFalse();
        assertThat(participants.enregistrer(nouveau(autre.idSession(), "Alice")).idParticipant()).isNotNull();
    }

    @Test
    void les_pseudonymes_de_la_seance_sont_listes_sans_ceux_des_autres_seances() {
        Seance autre = fixtures.creerSeance(EtatSession.PREPAREE, false);
        participants.enregistrer(nouveau(seance.idSession(), "Alice"));
        participants.enregistrer(nouveau(seance.idSession(), "Bob"));
        participants.enregistrer(nouveau(autre.idSession(), "Chloé"));

        assertThat(participants.pseudonymesDeLaSession(seance.idSession())).containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    void les_participants_sont_comptes_par_seance() {
        Seance autre = fixtures.creerSeance(EtatSession.PREPAREE, false);
        assertThat(participants.compter(seance.idSession())).isZero();

        participants.enregistrer(nouveau(seance.idSession(), "Alice"));
        participants.enregistrer(nouveau(seance.idSession(), "Bob"));
        participants.enregistrer(nouveau(autre.idSession(), "Chloé"));

        assertThat(participants.compter(seance.idSession())).isEqualTo(2);
        assertThat(participants.compter(autre.idSession())).isEqualTo(1);
    }

    @Test
    void deux_pseudonymes_identiques_envoyes_au_meme_instant_le_second_est_refuse_par_la_base() {
        participants.enregistrer(nouveau(seance.idSession(), "Alice"));

        // Le cas d'usage n'a pas vu le doublon (les deux requêtes ont lu avant d'écrire) : la contrainte tranche.
        assertThatThrownBy(() -> participants.enregistrer(nouveau(seance.idSession(), "Alice")))
                .isInstanceOfSatisfying(PseudonymeDejaPrisException.class, e -> {
                    assertThat(e.suggestion()).isNull();
                    assertThat(e).hasMessage("Ce pseudonyme est déjà pris.");
                });
        assertThat(participants.compter(seance.idSession())).isEqualTo(1);
    }

    @Test
    void le_refus_de_la_base_ne_laisse_ni_pseudonyme_ni_empreinte_dans_les_logs(CapturedOutput sortie) {
        ParticipantEntrant premier = nouveau(seance.idSession(), "PseudoSecret");
        participants.enregistrer(premier);

        assertThatThrownBy(() -> participants.enregistrer(nouveau(seance.idSession(), "PseudoSecret")))
                .isInstanceOf(PseudonymeDejaPrisException.class);
        // Même violation d'une autre contrainte : PostgreSQL recopie aussi l'empreinte du jeton dans son message.
        assertThatThrownBy(() -> participants.enregistrer(ParticipantEntrant.rejoindre(seance.idSession(),
                Pseudonyme.de("AutreSecret"), premier.empreinteJeton(), LocalDateTime.now())))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(sortie.getAll())
                .doesNotContain("PseudoSecret")
                .doesNotContain("AutreSecret")
                .doesNotContain(premier.empreinteJeton());
    }

    @Test
    void une_autre_violation_de_contrainte_n_est_pas_prise_pour_un_pseudonyme_deja_pris() {
        String empreinte = JetonParticipant.empreinte("meme-jeton");
        participants.enregistrer(ParticipantEntrant.rejoindre(seance.idSession(), Pseudonyme.de("Alice"), empreinte,
                LocalDateTime.now()));

        // Même empreinte de jeton (contrainte UNIQUE de jeton_reconnexion), pseudonymes différents.
        assertThatThrownBy(() -> participants.enregistrer(ParticipantEntrant.rejoindre(seance.idSession(),
                Pseudonyme.de("Bob"), empreinte, LocalDateTime.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
