package fr.iutmontreuil.quizzard.features.seance.application;

import static fr.iutmontreuil.quizzard.features.seance.application.Doublures.CODE;
import static fr.iutmontreuil.quizzard.features.seance.application.Doublures.ID_PARTICIPANT;
import static fr.iutmontreuil.quizzard.features.seance.application.Doublures.ID_QUESTION;
import static fr.iutmontreuil.quizzard.features.seance.application.Doublures.ID_SESSION;
import static fr.iutmontreuil.quizzard.features.seance.application.Doublures.JETON;
import static fr.iutmontreuil.quizzard.features.seance.application.Doublures.MAINTENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.iutmontreuil.quizzard.features.seance.application.dto.ReponseEnregistree;
import fr.iutmontreuil.quizzard.features.seance.application.dto.RepondreQuestionDto;
import fr.iutmontreuil.quizzard.features.seance.domain.DejaReponduException;
import fr.iutmontreuil.quizzard.features.seance.domain.EtatSession;
import fr.iutmontreuil.quizzard.features.seance.domain.HorsDelaiException;
import fr.iutmontreuil.quizzard.features.seance.domain.Participant;
import fr.iutmontreuil.quizzard.features.seance.domain.ParticipantInvalideException;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionCloseException;
import fr.iutmontreuil.quizzard.features.seance.domain.ReponseInvalideException;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionQuiz;

/** Chaque ligne du tableau des cas d'erreur de l'UC9, et le scénario nominal. */
class RepondreQuestionUseCaseTest {

    private final Doublures.Sessions sessions = new Doublures.Sessions();
    private final Doublures.Participants participants = new Doublures.Participants();
    private final Doublures.Questions questions = new Doublures.Questions();
    private final Doublures.Reponses reponses = new Doublures.Reponses();
    private final Doublures.Diffuseur diffuseur = new Doublures.Diffuseur();
    private final Doublures.Journal journal = new Doublures.Journal();
    private RepondreQuestionUseCase useCase;

    @BeforeEach
    void preparer() {
        sessions.parCode.put(CODE, Doublures.seanceEnCours());
        participants.ajouter(JETON, new Participant(ID_PARTICIPANT, ID_SESSION, 0));
        questions.parId.put(ID_QUESTION, Doublures.qcm());
        useCase = new RepondreQuestionUseCase(new IdentificationParticipant(sessions, participants), participants,
                questions, reponses, diffuseur, journal, Doublures.HORLOGE);
    }

    private static RepondreQuestionDto qcm(String jeton, Long idQuestion, Long... propositions) {
        return new RepondreQuestionDto(CODE, jeton, idQuestion, List.of(propositions), null, null);
    }

    @Test
    void une_reponse_juste_en_5_secondes_rapporte_875_points_et_est_signalee_a_l_enseignant() {
        ReponseEnregistree resultat = useCase.executer(qcm(JETON, ID_QUESTION, 41L));

        assertThat(resultat.idQuestion()).isEqualTo(ID_QUESTION);
        assertThat(resultat.horodatageServeur()).isEqualTo(MAINTENANT);
        assertThat(reponses.enregistrees).singleElement().satisfies(r -> {
            assertThat(r.pointsCalcules()).isEqualTo(875);
            assertThat(r.tempsReponseMs()).isEqualTo(5_000);
        });
        assertThat(participants.pointsAjoutes).containsEntry(ID_PARTICIPANT, 875);
        assertThat(diffuseur.signales).containsExactly(CODE + "/" + ID_QUESTION);
    }

    @Test
    void un_jeton_inconnu_est_refuse_avec_jeton_invalide() {
        assertThatThrownBy(() -> useCase.executer(qcm("autre-jeton", ID_QUESTION, 41L)))
                .isInstanceOf(ParticipantInvalideException.class);
        assertThat(reponses.enregistrees).isEmpty();
    }

    @Test
    void un_jeton_absent_est_refuse_avec_jeton_invalide() {
        assertThatThrownBy(() -> useCase.executer(qcm(null, ID_QUESTION, 41L)))
                .isInstanceOf(ParticipantInvalideException.class);
    }

    @Test
    void le_jeton_d_une_autre_seance_est_refuse_avec_jeton_invalide() {
        participants.ajouter("jeton-autre-seance", new Participant(11L, 2L, 0));
        assertThatThrownBy(() -> useCase.executer(qcm("jeton-autre-seance", ID_QUESTION, 41L)))
                .isInstanceOf(ParticipantInvalideException.class);
    }

    @Test
    void une_seance_close_refuse_la_reponse_avec_question_close() {
        sessions.parCode.put(CODE, new SessionQuiz(ID_SESSION, CODE, EtatSession.CLOTUREE, false, 1, ID_QUESTION,
                MAINTENANT.plusSeconds(15)));
        assertThatThrownBy(() -> useCase.executer(qcm(JETON, ID_QUESTION, 41L)))
                .isInstanceOf(QuestionCloseException.class);
    }

    @Test
    void une_reponse_a_une_question_qui_n_est_pas_ouverte_est_refusee_avec_question_close() {
        assertThatThrownBy(() -> useCase.executer(qcm(JETON, 99L, 41L)))
                .isInstanceOf(QuestionCloseException.class);
    }

    @Test
    void une_reponse_apres_l_echeance_est_refusee_et_tracee_sans_donnee_personnelle() {
        sessions.parCode.put(CODE, new SessionQuiz(ID_SESSION, CODE, EtatSession.EN_COURS, false, 1, ID_QUESTION,
                MAINTENANT.minusSeconds(1)));
        assertThatThrownBy(() -> useCase.executer(qcm(JETON, ID_QUESTION, 41L)))
                .isInstanceOf(HorsDelaiException.class);
        assertThat(journal.refus).containsExactly("reponse_hors_delai");
        assertThat(reponses.enregistrees).isEmpty();
    }

    @Test
    void une_seconde_reponse_est_refusee_avec_deja_repondu() {
        useCase.executer(qcm(JETON, ID_QUESTION, 41L));
        assertThatThrownBy(() -> useCase.executer(qcm(JETON, ID_QUESTION, 42L)))
                .isInstanceOf(DejaReponduException.class);
        assertThat(journal.refus).containsExactly("reponse_deja_enregistree");
        assertThat(reponses.enregistrees).hasSize(1);
        assertThat(participants.pointsAjoutes).containsEntry(ID_PARTICIPANT, 875);
    }

    @Test
    void un_contenu_incoherent_est_refuse_avec_reponse_invalide() {
        assertThatThrownBy(() -> useCase.executer(qcm(JETON, ID_QUESTION, 77L)))
                .isInstanceOf(ReponseInvalideException.class);
        assertThat(reponses.enregistrees).isEmpty();
        assertThat(diffuseur.signales).isEmpty();
    }
}
