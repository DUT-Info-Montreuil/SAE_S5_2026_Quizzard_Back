package fr.iutmontreuil.quizzard.features.seance.domain;

import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.ID_ENSEIGNANT;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.qcm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class SeanceAnimeeTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 25, 14, 0, 0, 123_456_789);
    private static final JetonSession JETON = new JetonSession("code-de-test");

    @Test
    void une_seance_lancee_est_preparee_sans_question_et_reprend_le_mode_note_du_quiz() {
        SeanceAnimee seance = SeanceAnimee.ouvrir(FabriqueLancement.quizNote(qcm(1, 3)), ID_ENSEIGNANT, JETON, true,
                MAINTENANT);

        assertThat(seance.idSession()).isNull();
        assertThat(seance.etat()).isEqualTo(EtatSession.PREPAREE);
        assertThat(seance.positionCourante()).isZero();
        assertThat(seance.modeNote()).isTrue();
        assertThat(seance.identiteNonVerifieeAutorisee()).isTrue();
        assertThat(seance.dateLancement()).isEqualTo(LocalDateTime.of(2026, 11, 25, 14, 0, 0, 123_000_000));
        assertThat(seance.aUneQuestionOuverte()).isFalse();
    }

    @Test
    void un_quiz_non_note_n_autorise_jamais_l_identite_non_verifiee() {
        SeanceAnimee seance = SeanceAnimee.ouvrir(FabriqueLancement.quiz(qcm(1, 3)), ID_ENSEIGNANT, JETON, true,
                MAINTENANT);

        assertThat(seance.identiteNonVerifieeAutorisee()).isFalse();
    }

    @Test
    void un_parametre_obligatoire_absent_est_une_erreur_de_programmation() {
        assertThatThrownBy(() -> SeanceAnimee.ouvrir(null, ID_ENSEIGNANT, JETON, false, MAINTENANT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SeanceAnimee.ouvrir(FabriqueLancement.quiz(), ID_ENSEIGNANT, null, false,
                MAINTENANT)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ouvrir_une_question_fixe_son_echeance_sur_l_horloge_du_serveur() {
        SeanceAnimee seance = seance();

        seance.ouvrirQuestionSuivante(20, MAINTENANT);

        assertThat(seance.etat()).isEqualTo(EtatSession.EN_COURS);
        assertThat(seance.positionCourante()).isEqualTo(1);
        assertThat(seance.echeanceQuestion()).isEqualTo(LocalDateTime.of(2026, 11, 25, 14, 0, 20, 123_000_000));
        assertThat(seance.aUneQuestionOuverte()).isTrue();
        assertThat(seance.echeanceAtteinte(seance.echeanceQuestion().minusNanos(1))).isFalse();
        assertThat(seance.echeanceAtteinte(seance.echeanceQuestion())).isTrue();
    }

    @Test
    void on_ne_passe_pas_a_la_suivante_tant_qu_une_question_est_ouverte() {
        SeanceAnimee seance = seance();
        seance.ouvrirQuestionSuivante(20, MAINTENANT);

        assertThatThrownBy(() -> seance.ouvrirQuestionSuivante(20, MAINTENANT))
                .isInstanceOf(QuestionDejaOuverteException.class);
    }

    @Test
    void clore_une_question_efface_l_echeance_et_garde_la_position() {
        SeanceAnimee seance = seance();
        seance.ouvrirQuestionSuivante(20, MAINTENANT);

        seance.cloreQuestion();

        assertThat(seance.etat()).isEqualTo(EtatSession.EN_COURS);
        assertThat(seance.positionCourante()).isEqualTo(1);
        assertThat(seance.echeanceQuestion()).isNull();
        assertThatThrownBy(seance::cloreQuestion).isInstanceOf(AucuneQuestionOuverteException.class);
    }

    @Test
    void une_seance_close_n_accepte_plus_rien() {
        SeanceAnimee seance = seance();
        seance.ouvrirQuestionSuivante(20, MAINTENANT);

        seance.cloturer(MAINTENANT.plusMinutes(5));

        assertThat(seance.etat()).isEqualTo(EtatSession.CLOTUREE);
        assertThat(seance.estTerminee()).isTrue();
        assertThat(seance.echeanceQuestion()).isNull();
        assertThat(seance.dateCloture()).isNotNull();
        assertThatThrownBy(() -> seance.cloturer(MAINTENANT)).isInstanceOf(SeanceTermineeException.class);
        assertThatThrownBy(() -> seance.ouvrirQuestionSuivante(20, MAINTENANT))
                .isInstanceOf(SeanceTermineeException.class);
        assertThatThrownBy(seance::cloreQuestion).isInstanceOf(AucuneQuestionOuverteException.class);
    }

    @Test
    void une_seance_peut_etre_abandonnee_avant_la_premiere_question() {
        SeanceAnimee seance = seance();

        seance.cloturer(MAINTENANT);

        assertThat(seance.etat()).isEqualTo(EtatSession.CLOTUREE);
    }

    @Test
    void la_seance_d_un_autre_enseignant_est_refusee() {
        assertThatThrownBy(() -> seance().verifierProprietaire(ID_ENSEIGNANT + 1))
                .isInstanceOf(AccesSeanceRefuseException.class);
    }

    @Test
    void le_jeton_ne_se_montre_pas_et_tient_dans_la_colonne() {
        assertThat(JETON.toString()).doesNotContain("code-de-test");
        assertThatThrownBy(() -> new JetonSession("x".repeat(JetonSession.LONGUEUR_MAX + 1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JetonSession(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    private static SeanceAnimee seance() {
        return SeanceAnimee.ouvrir(FabriqueLancement.quiz(qcm(1, 3)), ID_ENSEIGNANT, JETON, false, MAINTENANT);
    }
}
