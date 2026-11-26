package fr.iutmontreuil.quizzard.features.seance.domain;

import static fr.iutmontreuil.quizzard.features.seance.domain.Fabrique.MAINTENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/** Une réponse n'est acceptée que pour la question ouverte d'une séance en cours, avant l'échéance (DESIGN.md §4). */
class SessionQuizTest {

    private static final LocalDateTime ECHEANCE = MAINTENANT.plusSeconds(15);

    private static SessionQuiz seance(EtatSession etat, Long idQuestionOuverte, LocalDateTime echeance) {
        return new SessionQuiz(1L, "ABC123", etat, false, 1, idQuestionOuverte, echeance);
    }

    @Test
    void le_temps_de_reponse_est_la_duree_moins_le_temps_restant() {
        // Question de 20 s dont l'échéance est dans 15 s : ouverte depuis 5 s.
        assertThat(seance(EtatSession.EN_COURS, 12L, ECHEANCE).verifierReponseAcceptee(12L, 20, MAINTENANT))
                .isEqualTo(5_000);
    }

    @Test
    void une_reponse_pile_a_l_echeance_est_acceptee() {
        assertThat(seance(EtatSession.EN_COURS, 12L, MAINTENANT).verifierReponseAcceptee(12L, 20, MAINTENANT))
                .isEqualTo(20_000);
    }

    @Test
    void une_reponse_hors_delai_est_refusee() {
        assertThatThrownBy(() -> seance(EtatSession.EN_COURS, 12L, MAINTENANT.minusNanos(1_000_000))
                .verifierReponseAcceptee(12L, 20, MAINTENANT))
                .isInstanceOf(HorsDelaiException.class);
    }

    @Test
    void une_reponse_a_une_autre_question_que_la_question_ouverte_est_refusee() {
        assertThatThrownBy(() -> seance(EtatSession.EN_COURS, 12L, ECHEANCE)
                .verifierReponseAcceptee(13L, 20, MAINTENANT))
                .isInstanceOf(QuestionCloseException.class);
    }

    @Test
    void une_reponse_dans_une_seance_close_ou_pas_commencee_est_refusee() {
        assertThatThrownBy(() -> seance(EtatSession.CLOTUREE, 12L, ECHEANCE)
                .verifierReponseAcceptee(12L, 20, MAINTENANT))
                .isInstanceOf(QuestionCloseException.class);
        assertThatThrownBy(() -> seance(EtatSession.PREPAREE, null, null)
                .verifierReponseAcceptee(12L, 20, MAINTENANT))
                .isInstanceOf(QuestionCloseException.class);
    }

    @Test
    void une_reponse_sans_question_ouverte_est_refusee() {
        assertThatThrownBy(() -> seance(EtatSession.EN_COURS, 12L, null)
                .verifierReponseAcceptee(12L, 20, MAINTENANT))
                .isInstanceOf(QuestionCloseException.class);
    }

    @Test
    void chaque_exception_porte_le_code_et_le_message_de_l_uc9() {
        assertThat(new ParticipantInvalideException().code()).isEqualTo("JETON_INVALIDE");
        assertThat(new QuestionCloseException()).hasMessage("Cette session est terminée.");
        assertThat(new HorsDelaiException()).hasMessage("Temps écoulé : réponse non prise en compte.");
        assertThat(new DejaReponduException().code()).isEqualTo("DEJA_REPONDU");
        assertThat(new ReponseInvalideException()).hasMessage("Réponse incomplète, vérifie ta saisie.");
    }
}
