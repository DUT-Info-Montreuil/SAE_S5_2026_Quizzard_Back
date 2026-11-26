package fr.iutmontreuil.quizzard.features.cours.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Règles de l'UC3 portées par le domaine : emplacement, version modifiable, valeurs par défaut. */
class CoursDomaineTest {

    private final VersionCours version = new VersionCours(10L, 2, 12);

    @Test
    void un_emplacement_entre_0_et_nb_pages_et_jusqu_au_nombre_de_pages_au_meme_endroit_est_valide() {
        assertThatNoException().isThrownBy(() -> version.verifierEmplacement(0, 0, 0));
        assertThatNoException().isThrownBy(() -> version.verifierEmplacement(12, 0, 0));
        assertThatNoException().isThrownBy(() -> version.verifierEmplacement(4, 2, 2));
    }

    @Test
    void un_emplacement_hors_du_cours_est_refuse() {
        assertThatThrownBy(() -> version.verifierEmplacement(13, 0, 0)).isInstanceOf(EmplacementInvalideException.class);
        assertThatThrownBy(() -> version.verifierEmplacement(-1, 0, 0)).isInstanceOf(EmplacementInvalideException.class);
        assertThatThrownBy(() -> version.verifierEmplacement(4, 3, 2)).isInstanceOf(EmplacementInvalideException.class);
        assertThatThrownBy(() -> version.verifierEmplacement(4, -1, 0)).isInstanceOf(EmplacementInvalideException.class);
    }

    @Test
    void seule_la_derniere_version_d_un_cours_non_archive_est_modifiable() {
        assertThat(new Cours(1L, 7L, false, version).versionModifiable(10L)).isEqualTo(version);
        assertThatThrownBy(() -> new Cours(1L, 7L, false, version).versionModifiable(9L))
                .isInstanceOf(VersionPerimeeException.class);
        assertThatThrownBy(() -> new Cours(1L, 7L, true, version).versionModifiable(10L))
                .isInstanceOf(CoursArchiveException.class);
    }

    @Test
    void un_nouveau_quiz_a_les_reglages_par_defaut_du_dossier() {
        Quiz quiz = Quiz.nouveau(1L, 3);
        assertThat(quiz.titre()).isEqualTo("Quiz 3");
        assertThat(quiz.estNote()).isFalse();
        assertThat(quiz.dureeParDefautSec()).isEqualTo(20);
        assertThat(quiz.passageAutomatique()).isFalse();
        assertThat(quiz.affichageCours()).isEqualTo("PROJETE_SEUL");
        assertThat(quiz.estNouveau()).isTrue();
        assertThatIllegalArgumentException().isThrownBy(() -> Quiz.nouveau(1L, 0));
    }

    @Test
    void la_question_ajoutee_est_un_qcm_vierge_avec_le_bareme_par_defaut() {
        Question question = Quiz.nouveau(1L, 1).ajouterQuestionVierge();
        assertThat(question.type()).isEqualTo("QCM");
        assertThat(question.enonce()).isEmpty();
        assertThat(question.dureeSec()).isNull();
        assertThat(question.pointsMax()).isEqualTo(1000);
        assertThat(question.decroissanceTemps()).isTrue();
        assertThat(question.pointsPartiels()).isFalse();
    }

    @Test
    void un_quiz_archive_ne_recoit_pas_de_question() {
        Quiz archive = new Quiz(5L, 1L, "Quiz 1", false, 20, false, "PROJETE_SEUL", true);
        assertThatIllegalStateException().isThrownBy(archive::ajouterQuestionVierge);
    }

    @Test
    void le_repere_de_texte_est_nettoye_et_borne_a_300_caracteres() {
        assertThat(PageQuiz.creer(10L, 2, 0, "  fin   de\n la page ", 3L).repereTexte()).isEqualTo("fin de la page");
        assertThat(PageQuiz.creer(10L, 2, 0, "   ", 3L).repereTexte()).isNull();
        assertThat(PageQuiz.creer(10L, 2, 0, "a".repeat(400), 3L).repereTexte()).hasSize(300);
        assertThat(PageQuiz.creer(10L, 2, 0, null, 3L).valide()).isTrue();
        assertThatIllegalArgumentException().isThrownBy(() -> PageQuiz.creer(10L, 2, 0, null, null));
    }

    @Test
    void chaque_exception_porte_le_code_et_le_message_de_l_uc3() {
        assertThat(new CoursIntrouvableException()).hasMessage("Cours introuvable.");
        assertThat(new CoursArchiveException().code()).isEqualTo("COURS_ARCHIVE");
        assertThat(new VersionPerimeeException())
                .hasMessage("Une nouvelle version du cours a été importée : rechargez l'éditeur.");
        assertThat(new EmplacementInvalideException().code()).isEqualTo("EMPLACEMENT_INVALIDE");
        assertThat(new QuizIntrouvableException().code()).isEqualTo("QUIZ_INTROUVABLE");
        assertThat(new QuizEnSeanceException())
                .hasMessage("Ce quiz est en cours de séance : réessayez après sa clôture.");
    }
}
