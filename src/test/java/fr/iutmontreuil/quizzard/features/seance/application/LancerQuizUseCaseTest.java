package fr.iutmontreuil.quizzard.features.seance.application;

import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.ID_ENSEIGNANT;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.ID_QUIZ;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.incomplete;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.qcm;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.quiz;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.quizNote;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.relier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import fr.iutmontreuil.quizzard.features.seance.application.DoublesLancement.Journal.Ligne;
import fr.iutmontreuil.quizzard.features.seance.application.dto.LancerQuizDto;
import fr.iutmontreuil.quizzard.features.seance.application.dto.SeanceOuverte;
import fr.iutmontreuil.quizzard.features.seance.application.dto.SessionLancee;
import fr.iutmontreuil.quizzard.features.seance.domain.AccesQuizRefuseException;
import fr.iutmontreuil.quizzard.features.seance.domain.EtatSession;
import fr.iutmontreuil.quizzard.features.seance.domain.QuizIncompletException;
import fr.iutmontreuil.quizzard.features.seance.domain.QuizIntrouvableException;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionDejaOuverteException;

class LancerQuizUseCaseTest {

    private final DoublesLancement doubles = new DoublesLancement();

    @Test
    void le_lancement_cree_la_seance_le_lien_et_le_qr_code_dans_l_ordre_du_dossier() {
        doubles.quizzes.ajouter(quiz(qcm(1, 3), relier(2, 5)));

        SessionLancee lancee = lancer(false);

        assertThat(lancee.seance().idSession()).isEqualTo(1L);
        assertThat(lancee.seance().etat()).isEqualTo(EtatSession.PREPAREE);
        assertThat(lancee.titre()).isEqualTo("Quiz 1");
        assertThat(lancee.nbQuestions()).isEqualTo(2);
        assertThat(lancee.lienAcces()).isEqualTo(DoublesLancement.URL_FRONT + "/rejoindre/jeton-1");
        assertThat(lancee.qrCode().png()).isEqualTo(lancee.lienAcces().getBytes());
        assertThat(doubles.quizzes.verrouillees).containsExactly(1L, 2L);
        // Un quiz non noté n'interroge pas l'annuaire ; le verrou précède la recherche d'une séance ouverte.
        assertThat(doubles.appels).containsExactly("verrou", "chercherOuverte", "enregistrer", "qrCode",
                "ouvrirCanal", "journal");
    }

    @Test
    void le_lancement_est_une_action_sensible_sans_code_d_acces() {
        doubles.quizzes.ajouter(quiz(qcm(1, 3)));

        lancer(false);

        assertThat(doubles.journal.lignes).singleElement().satisfies(ligne -> {
            assertThat(ligne.type()).isEqualTo("actionSensible");
            assertThat(ligne.nom()).isEqualTo("quiz_lance");
            assertThat(ligne.champs()).containsEntry("cible", 1L).containsEntry("idQuiz", ID_QUIZ)
                    .containsEntry("modeNote", false).containsEntry("nbQuestions", 1);
            assertThat(ligne.champs().toString()).doesNotContain("jeton-1");
        });
    }

    @Test
    void un_quiz_inconnu_est_introuvable() {
        assertThatThrownBy(() -> lancer(false)).isInstanceOf(QuizIntrouvableException.class);
        rienNAEteCree();
    }

    @Test
    void le_quiz_d_un_autre_enseignant_est_refuse_avant_tout_autre_controle() {
        doubles.quizzes.ajouter(quiz());

        assertThatThrownBy(() -> doubles.lancer().executer(new LancerQuizDto(ID_QUIZ, 99L, false)))
                .isInstanceOf(AccesQuizRefuseException.class);
        rienNAEteCree();
    }

    @Test
    void un_quiz_incomplet_est_refuse_et_trace_dans_les_logs_seulement() {
        doubles.quizzes.ajouter(quiz(qcm(1, 3), incomplete(2, 5)));

        assertThatThrownBy(() -> lancer(false)).isInstanceOf(QuizIncompletException.class);

        rienNAEteCree();
        assertThat(doubles.journal.lignes).extracting(Ligne::type, Ligne::nom)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("regleRefusee",
                        "lancement_refuse_quiz_incomplet"));
    }

    @Test
    void une_seance_deja_ouverte_bloque_le_lancement_et_nomme_le_cours() {
        doubles.quizzes.ajouter(quiz(qcm(1, 3)));
        doubles.seances.ouverte = new SeanceOuverte(7L, "Réseaux");

        assertThatThrownBy(() -> lancer(false))
                .isInstanceOf(SessionDejaOuverteException.class)
                .hasMessage("Une session est déjà ouverte pour le cours « Réseaux ». La reprendre ou la terminer ?");

        rienNAEteCree();
        assertThat(doubles.journal.lignes).extracting(Ligne::nom)
                .containsExactly("lancement_refuse_session_ouverte");
    }

    @Test
    void un_quiz_note_sans_annuaire_exige_l_autorisation_de_l_enseignant() {
        doubles.quizzes.ajouter(quizNote(qcm(1, 3)));
        doubles.annuaire.disponible = false;

        assertThatThrownBy(() -> lancer(false)).isInstanceOf(AnnuaireIndisponibleException.class);
        rienNAEteCree();

        SessionLancee lancee = lancer(true);
        assertThat(lancee.seance().modeNote()).isTrue();
        assertThat(lancee.seance().identiteNonVerifieeAutorisee()).isTrue();
    }

    @Test
    void un_quiz_note_avec_annuaire_n_ouvre_pas_l_identite_non_verifiee() {
        doubles.quizzes.ajouter(quizNote(qcm(1, 3)));

        SessionLancee lancee = lancer(true);

        assertThat(doubles.appels).contains("annuaire");
        assertThat(lancee.seance().identiteNonVerifieeAutorisee()).isFalse();
    }

    @Test
    void une_panne_du_qr_code_interrompt_le_lancement_avant_le_canal_et_le_journal() {
        doubles.quizzes.ajouter(quiz(qcm(1, 3)));
        doubles.panneQrCode = new GenerationAccesException("panne", null);

        assertThatThrownBy(() -> lancer(false)).isInstanceOf(GenerationAccesException.class);

        assertThat(doubles.appels).doesNotContain("ouvrirCanal", "journal");
    }

    @Test
    void une_panne_du_canal_temps_reel_empeche_la_trace_du_lancement() {
        doubles.quizzes.ajouter(quiz(qcm(1, 3)));
        doubles.diffuseur.panne = new IllegalStateException("canal");

        assertThatThrownBy(() -> lancer(false)).isInstanceOf(IllegalStateException.class);

        assertThat(doubles.appels).doesNotContain("journal");
    }

    private SessionLancee lancer(boolean autoriser) {
        return doubles.lancer().executer(new LancerQuizDto(ID_QUIZ, ID_ENSEIGNANT, autoriser));
    }

    private void rienNAEteCree() {
        assertThat(doubles.seances.parId).isEmpty();
        assertThat(doubles.quizzes.verrouillees).isEmpty();
        assertThat(doubles.appels).doesNotContain("enregistrer", "ouvrirCanal", "journal");
    }
}
