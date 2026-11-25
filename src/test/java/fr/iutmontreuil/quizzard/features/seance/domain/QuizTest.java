package fr.iutmontreuil.quizzard.features.seance.domain;

import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.ID_ENSEIGNANT;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.incomplete;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.libre;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.qcm;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.quiz;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.relier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class QuizTest {

    @Test
    void un_quiz_dont_toutes_les_questions_sont_completes_est_pret_a_lancer() {
        assertThatCode(() -> quiz(qcm(1, 3), relier(2, 5), libre(3, 7)).verifierPretALancer())
                .doesNotThrowAnyException();
    }

    @Test
    void un_quiz_sans_question_n_est_pas_pret() {
        assertThatThrownBy(() -> quiz().verifierPretALancer())
                .isInstanceOf(QuizIncompletException.class)
                .hasMessage("Le quiz ne contient aucune question à présenter. "
                        + "Insérez une page de quiz avant de lancer.");
    }

    @Test
    void une_question_incomplete_est_designee_par_sa_page() {
        assertThatThrownBy(() -> quiz(qcm(1, 3), incomplete(2, 6)).verifierPretALancer())
                .isInstanceOfSatisfying(QuizIncompletException.class, e -> {
                    assertThat(e.pages()).containsExactly(6);
                    assertThat(e.code()).isEqualTo("QUIZ_INCOMPLET");
                })
                .hasMessage("Le quiz contient une question incomplète (page 6). Complétez-la avant de lancer.");
    }

    @Test
    void toutes_les_pages_incompletes_sont_listees() {
        assertThatThrownBy(() -> quiz(incomplete(1, 2), incomplete(2, 9)).verifierPretALancer())
                .hasMessageContaining("(pages 2, 9)");
    }

    @Test
    void les_reponses_attendues_conditionnent_la_completude() {
        PropositionDeSeance juste = new PropositionDeSeance(1L, "A", true, 0);
        PropositionDeSeance fausse = new PropositionDeSeance(2L, "B", false, 1);

        assertThat(question(TypeQuestion.QCM, List.of(juste, fausse), List.of(), List.of()).estComplete()).isTrue();
        assertThat(question(TypeQuestion.QCM, List.of(juste), List.of(), List.of()).estComplete()).isFalse();
        assertThat(question(TypeQuestion.QCM, List.of(fausse, fausse), List.of(), List.of()).estComplete())
                .isFalse();
        assertThat(question(TypeQuestion.QCM, List.of(juste, new PropositionDeSeance(3L, "", false, 1)), List.of(),
                List.of()).estComplete()).isFalse();
        assertThat(question(TypeQuestion.POINTS_A_RELIER, List.of(),
                List.of(new PaireDeSeance(1L, "80", "HTTP")), List.of()).estComplete()).isFalse();
        assertThat(question(TypeQuestion.POINTS_A_RELIER, List.of(),
                List.of(new PaireDeSeance(1L, "80", "HTTP"), new PaireDeSeance(2L, "53", " ")), List.of())
                .estComplete()).isFalse();
        assertThat(question(TypeQuestion.REPONSE_LIBRE, List.of(), List.of(), List.of()).estComplete()).isFalse();
        assertThat(question(TypeQuestion.REPONSE_LIBRE, List.of(), List.of(),
                List.of(new MotifDeSeance("https", false))).estComplete()).isTrue();
    }

    @Test
    void le_quiz_d_un_autre_enseignant_est_refuse() {
        assertThatThrownBy(() -> quiz(qcm(1, 3)).verifierPropriete(ID_ENSEIGNANT + 1))
                .isInstanceOf(AccesQuizRefuseException.class)
                .hasMessage("Action non autorisée.");
        assertThatCode(() -> quiz(qcm(1, 3)).verifierPropriete(ID_ENSEIGNANT)).doesNotThrowAnyException();
    }

    @Test
    void les_questions_sont_numerotees_a_partir_de_1() {
        Quiz quiz = quiz(qcm(1, 3), relier(2, 5));

        assertThat(quiz.question(0)).isEmpty();
        assertThat(quiz.question(1)).get().extracting(QuestionDeSeance::idQuestion).isEqualTo(1L);
        assertThat(quiz.question(2)).get().extracting(QuestionDeSeance::idQuestion).isEqualTo(2L);
        assertThat(quiz.question(3)).isEmpty();
    }

    @Test
    void le_passage_automatique_attend_le_dernier_ecran_puis_le_delai_regle() {
        QuestionDeSeance avecEcrans = qcm(1, 3, new EcranIntermediaire(TypeEcran.CLASSEMENT, 1, 8),
                new EcranIntermediaire(TypeEcran.BONNE_REPONSE, 0, 0));

        assertThat(quiz(false, true, 4, avecEcrans).attenteAvantSuivante(avecEcrans))
                .isEqualTo(Duration.ofSeconds(12));
        assertThat(quiz(false, true, null, avecEcrans).attenteAvantSuivante(avecEcrans))
                .isEqualTo(Duration.ofSeconds(8 + Quiz.DELAI_PASSAGE_PAR_DEFAUT_SEC));
        assertThat(avecEcrans.ecrans()).extracting(EcranIntermediaire::rang).containsExactly(0, 1);
    }

    private static QuestionDeSeance question(TypeQuestion type, List<PropositionDeSeance> propositions,
                                             List<PaireDeSeance> paires, List<MotifDeSeance> motifs) {
        return new QuestionDeSeance(1L, type, "Énoncé", 20, 1, propositions, paires, motifs, List.of());
    }
}
