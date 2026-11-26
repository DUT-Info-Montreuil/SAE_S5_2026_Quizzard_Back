package fr.iutmontreuil.quizzard.features.seance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import fr.iutmontreuil.quizzard.features.seance.application.DoublesSeance.DiffuseurEnregistreur;
import fr.iutmontreuil.quizzard.features.seance.application.DoublesSeance.GenerateurFixe;
import fr.iutmontreuil.quizzard.features.seance.application.DoublesSeance.JournalEnregistreur;
import fr.iutmontreuil.quizzard.features.seance.application.DoublesSeance.ParticipantsEnMemoire;
import fr.iutmontreuil.quizzard.features.seance.application.DoublesSeance.SessionsEnMemoire;
import fr.iutmontreuil.quizzard.features.seance.application.dto.ParticipantRejoint;
import fr.iutmontreuil.quizzard.features.seance.application.dto.RejoindreSessionDto;
import fr.iutmontreuil.quizzard.features.seance.domain.ConnexionIutRequiseException;
import fr.iutmontreuil.quizzard.features.seance.domain.EtatSession;
import fr.iutmontreuil.quizzard.features.seance.domain.ParticipantEntrant;
import fr.iutmontreuil.quizzard.features.seance.domain.PseudonymeDejaPrisException;
import fr.iutmontreuil.quizzard.features.seance.domain.PseudonymeInvalideException;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionCloseException;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionIntrouvableException;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionQuiz;

class RejoindreSessionUseCaseTest {

    private static final String CODE = "code-de-la-seance";
    private static final Long ID_SESSION = 5L;
    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");
    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 24, 10, 30, 0);
    private static final Clock HORLOGE = Clock.fixed(MAINTENANT.atZone(ZONE).toInstant(), ZONE);

    private SessionsEnMemoire sessions;
    private ParticipantsEnMemoire participants;
    private DiffuseurEnregistreur diffuseur;
    private JournalEnregistreur journal;
    private RejoindreSessionUseCase casUsage;

    @BeforeEach
    void preparer() {
        sessions = new SessionsEnMemoire().avec(session(EtatSession.PREPAREE, false));
        participants = new ParticipantsEnMemoire();
        diffuseur = new DiffuseurEnregistreur();
        journal = new JournalEnregistreur();
        casUsage = new RejoindreSessionUseCase(sessions, participants, new GenerateurFixe(), diffuseur, journal,
                HORLOGE);
    }

    private static SessionQuiz session(EtatSession etat, boolean estNotee) {
        return new SessionQuiz(ID_SESSION, CODE, etat, estNotee, 0, null, null);
    }

    private ParticipantRejoint rejoindre(String pseudonyme) {
        return casUsage.executer(new RejoindreSessionDto(CODE, pseudonyme));
    }

    // ---- Scénario nominal ----

    @Test
    void un_etudiant_rejoint_la_seance_et_recoit_son_jeton_en_clair() {
        ParticipantRejoint resultat = rejoindre("Alice");

        assertThat(resultat.idParticipant()).isNotNull();
        assertThat(resultat.jeton()).isEqualTo(GenerateurFixe.VALEUR);
        assertThat(resultat.pseudonyme()).isEqualTo("Alice");
    }

    @Test
    void le_pseudonyme_retenu_est_le_pseudonyme_nettoye() {
        ParticipantRejoint resultat = rejoindre("  Jean   Pierre ");

        assertThat(resultat.pseudonyme()).isEqualTo("Jean Pierre");
    }

    @Test
    void seule_l_empreinte_du_jeton_est_enregistree() {
        rejoindre("Alice");

        ParticipantEntrant enregistre = participants.enregistres.getFirst();
        assertThat(enregistre.empreinteJeton()).isEqualTo(GenerateurFixe.EMPREINTE);
        assertThat(enregistre.toString()).doesNotContain(GenerateurFixe.VALEUR);
    }

    @Test
    void le_participant_enregistre_a_un_score_a_zero_et_la_date_de_l_horloge() {
        rejoindre("Alice");

        ParticipantEntrant enregistre = participants.enregistres.getFirst();
        assertThat(enregistre.idSession()).isEqualTo(ID_SESSION);
        assertThat(enregistre.scoreTotal()).isZero();
        assertThat(enregistre.identiteVerifiee()).isFalse();
        assertThat(enregistre.dateEntree()).isEqualTo(MAINTENANT);
        assertThat(enregistre.derniereActivite()).isEqualTo(MAINTENANT);
    }

    @ParameterizedTest
    @EnumSource(value = EtatSession.class, names = {"PREPAREE", "EN_COURS"})
    void on_peut_rejoindre_avant_la_premiere_question_et_en_cours_de_quiz(EtatSession etat) {
        sessions.avec(session(etat, false));

        ParticipantRejoint resultat = rejoindre("Alice");

        assertThat(resultat.idParticipant()).isNotNull();
        assertThat(participants.enregistres).hasSize(1);
    }

    @Test
    void le_nombre_de_connectes_est_envoye_a_l_ecran_de_l_enseignant_avec_le_code_d_acces() {
        rejoindre("Alice");
        rejoindre("Bob");

        assertThat(diffuseur.envois).containsExactly(
                new DiffuseurEnregistreur.Envoi(CODE, 1),
                new DiffuseurEnregistreur.Envoi(CODE, 2));
    }

    @Test
    void l_entree_reussie_est_tracee_sans_pseudonyme_ni_jeton() {
        ParticipantRejoint resultat = rejoindre("Alice");

        assertThat(journal.evenements).hasSize(1);
        JournalEnregistreur.Ligne ligne = journal.evenements.getFirst();
        assertThat(ligne.nom()).isEqualTo("participant_entre");
        assertThat(ligne.champs()).containsOnly(
                java.util.Map.entry("idSession", ID_SESSION),
                java.util.Map.entry("idParticipant", resultat.idParticipant()));
        assertThat(journal.actionsSensibles).isEmpty();
    }

    // ---- Refus ----

    @Test
    void un_code_d_acces_inconnu_est_refuse_sans_rien_enregistrer() {
        assertThatThrownBy(() -> casUsage.executer(new RejoindreSessionDto("inconnu", "Alice")))
                .isInstanceOf(SessionIntrouvableException.class);

        assertThat(participants.enregistres).isEmpty();
        assertThat(diffuseur.envois).isEmpty();
    }

    @Test
    void une_seance_cloturee_refuse_l_entree_et_trace_le_refus() {
        sessions.avec(session(EtatSession.CLOTUREE, false));

        assertThatThrownBy(() -> rejoindre("Alice")).isInstanceOf(SessionCloseException.class);

        assertThat(participants.enregistres).isEmpty();
        assertThat(diffuseur.envois).isEmpty();
        assertThat(journal.refus).singleElement().satisfies(ligne -> {
            assertThat(ligne.nom()).isEqualTo("entree_refusee_session_close");
            assertThat(ligne.champs()).containsOnly(java.util.Map.entry("idSession", ID_SESSION));
        });
    }

    @Test
    void un_quiz_note_refuse_l_entree_sans_compte_et_trace_le_refus() {
        sessions.avec(session(EtatSession.PREPAREE, true));

        assertThatThrownBy(() -> rejoindre("Alice")).isInstanceOf(ConnexionIutRequiseException.class);

        assertThat(participants.enregistres).isEmpty();
        assertThat(diffuseur.envois).isEmpty();
        assertThat(journal.refus).singleElement().satisfies(ligne -> {
            assertThat(ligne.nom()).isEqualTo("entree_refusee_quiz_note");
            assertThat(ligne.champs()).containsOnly(java.util.Map.entry("idSession", ID_SESSION));
        });
    }

    @Test
    void un_pseudonyme_invalide_est_refuse_sans_rien_enregistrer() {
        assertThatThrownBy(() -> rejoindre("A")).isInstanceOf(PseudonymeInvalideException.class);
        assertThatThrownBy(() -> rejoindre(null)).isInstanceOf(PseudonymeInvalideException.class);

        assertThat(participants.enregistres).isEmpty();
        assertThat(diffuseur.envois).isEmpty();
    }

    @Test
    void un_pseudonyme_deja_pris_est_refuse_avec_une_proposition() {
        participants.avecPseudonyme(ID_SESSION, "Alice");

        assertThatThrownBy(() -> rejoindre("Alice"))
                .isInstanceOfSatisfying(PseudonymeDejaPrisException.class,
                        e -> assertThat(e.suggestion()).isEqualTo("Alice2"))
                .hasMessage("Ce pseudonyme est déjà pris. Essaie celui qui t'est proposé.");

        assertThat(participants.enregistres).hasSize(1);
        assertThat(diffuseur.envois).isEmpty();
    }

    @Test
    void la_proposition_est_un_pseudonyme_libre() {
        participants.avecPseudonyme(ID_SESSION, "Alice").avecPseudonyme(ID_SESSION, "Alice2");

        assertThatThrownBy(() -> rejoindre("Alice"))
                .isInstanceOfSatisfying(PseudonymeDejaPrisException.class,
                        e -> assertThat(e.suggestion()).isEqualTo("Alice3"));
    }

    @Test
    void le_doublon_est_detecte_apres_nettoyage_du_pseudonyme() {
        participants.avecPseudonyme(ID_SESSION, "Alice");

        assertThatThrownBy(() -> rejoindre("  Alice "))
                .isInstanceOf(PseudonymeDejaPrisException.class);
    }

    @Test
    void le_meme_pseudonyme_dans_une_autre_seance_est_accepte() {
        participants.avecPseudonyme(99L, "Alice");

        assertThat(rejoindre("Alice").pseudonyme()).isEqualTo("Alice");
    }

    @Test
    void la_casse_distingue_deux_pseudonymes() {
        participants.avecPseudonyme(ID_SESSION, "Alice");

        assertThat(rejoindre("alice").pseudonyme()).isEqualTo("alice");
    }

    @Test
    void le_refus_pour_pseudonyme_pris_est_trace_sans_le_pseudonyme() {
        participants.avecPseudonyme(ID_SESSION, "Alice");

        assertThatThrownBy(() -> rejoindre("Alice")).isInstanceOf(PseudonymeDejaPrisException.class);

        assertThat(journal.refus).singleElement().satisfies(ligne -> {
            assertThat(ligne.nom()).isEqualTo("entree_refusee_pseudonyme_pris");
            assertThat(ligne.champs()).containsOnly(java.util.Map.entry("idSession", ID_SESSION));
        });
    }

    @Test
    void deux_etudiants_au_meme_instant_le_second_est_refuse_sans_proposition_et_sans_diffusion() {
        participants.quiRefuseCommeUnDoublonSimultane();

        assertThatThrownBy(() -> rejoindre("Alice"))
                .isInstanceOfSatisfying(PseudonymeDejaPrisException.class,
                        e -> assertThat(e.suggestion()).isNull())
                .hasMessage("Ce pseudonyme est déjà pris.");

        assertThat(diffuseur.envois).isEmpty();
        assertThat(journal.evenements).isEmpty();
    }

    @Test
    void l_horloge_est_celle_du_constructeur() {
        Clock autre = Clock.fixed(Instant.parse("2030-01-01T12:00:00Z"), ZoneId.of("UTC"));
        casUsage = new RejoindreSessionUseCase(sessions, participants, new GenerateurFixe(), diffuseur, journal, autre);

        rejoindre("Alice");

        assertThat(participants.enregistres.getFirst().dateEntree()).isEqualTo(LocalDateTime.of(2030, 1, 1, 12, 0));
    }
}
