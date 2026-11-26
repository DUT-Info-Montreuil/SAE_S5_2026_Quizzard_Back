package fr.iutmontreuil.quizzard.features.seance.application;

import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.ID_ENSEIGNANT;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.ID_QUIZ;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.incomplete;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.qcm;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.quiz;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.relier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.iutmontreuil.quizzard.features.seance.application.DoublesLancement.Minuterie.Tache;
import fr.iutmontreuil.quizzard.features.seance.application.dto.DeroulementSeance;
import fr.iutmontreuil.quizzard.features.seance.application.dto.LancerQuizDto;
import fr.iutmontreuil.quizzard.features.seance.application.dto.RepartitionReponses;
import fr.iutmontreuil.quizzard.features.seance.domain.AccesSeanceRefuseException;
import fr.iutmontreuil.quizzard.features.seance.domain.AucuneQuestionOuverteException;
import fr.iutmontreuil.quizzard.features.seance.domain.EcranIntermediaire;
import fr.iutmontreuil.quizzard.features.seance.domain.EtatSession;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionDejaOuverteException;
import fr.iutmontreuil.quizzard.features.seance.domain.Quiz;
import fr.iutmontreuil.quizzard.features.seance.domain.QuizIncompletException;
import fr.iutmontreuil.quizzard.features.seance.domain.ScoreParticipant;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceIntrouvableException;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceTermineeException;
import fr.iutmontreuil.quizzard.features.seance.domain.TypeEcran;

class DeroulementUseCaseTest {

    private final DoublesLancement doubles = new DoublesLancement();

    @Test
    void demarrer_ouvre_la_premiere_question_l_annonce_et_programme_sa_cloture() {
        Long id = lancer(quiz(qcm(1, 3), relier(2, 5)));

        DeroulementSeance etat = doubles.demarrer().executer(id, ID_ENSEIGNANT);

        assertThat(etat.seance().etat()).isEqualTo(EtatSession.EN_COURS);
        assertThat(etat.seance().positionCourante()).isEqualTo(1);
        assertThat(etat.tempsRestantMs()).isEqualTo(20_000);
        assertThat(etat.correction()).as("aucune correction pendant la question").isNull();
        assertThat(doubles.diffuseur.questions).singleElement().satisfies(q -> {
            assertThat(q.idQuestion()).isEqualTo(1L);
            assertThat(q.echeance()).isEqualTo(doubles.horloge.maintenant().plusSeconds(20));
        });
        assertThat(doubles.minuterie.taches).containsExactly(
                new Tache("cloture", id, 1, doubles.horloge.maintenant().plusSeconds(20)));
    }

    @Test
    void suivant_est_refuse_pendant_une_question_ouverte() {
        Long id = lancer(quiz(qcm(1, 3), relier(2, 5)));
        doubles.demarrer().executer(id, ID_ENSEIGNANT);

        assertThatThrownBy(() -> doubles.demarrer().executer(id, ID_ENSEIGNANT))
                .isInstanceOf(QuestionDejaOuverteException.class);
        assertThat(doubles.diffuseur.questions).hasSize(1);
    }

    @Test
    void suivant_apres_l_echeance_clot_la_question_oubliee_puis_ouvre_la_suivante() {
        Long id = lancer(quiz(qcm(1, 3), relier(2, 5)));
        doubles.demarrer().executer(id, ID_ENSEIGNANT);
        doubles.horloge.avancer(Duration.ofSeconds(20));

        doubles.demarrer().executer(id, ID_ENSEIGNANT);

        assertThat(doubles.appels).containsSubsequence("question", "correction", "question");
        assertThat(seance(id).positionCourante()).isEqualTo(2);
    }

    @Test
    void clore_arrete_les_reponses_et_publie_la_correction_et_les_resultats() {
        Long id = lancer(quiz(qcm(1, 3)));
        doubles.demarrer().executer(id, ID_ENSEIGNANT);
        doubles.suivi.repartition = new RepartitionReponses(2, Map.of(10L, 1, 11L, 1));
        doubles.suivi.scores = List.of(
                new ScoreParticipant(5L, 900, 2000, doubles.horloge.maintenant(), true, 900, 2000),
                new ScoreParticipant(6L, 0, 0, doubles.horloge.maintenant(), false, 0, 0));

        DeroulementSeance etat = doubles.clore().executer(id, ID_ENSEIGNANT);

        assertThat(etat.seance().aUneQuestionOuverte()).isFalse();
        assertThat(etat.correction().bonnesPropositions()).containsExactly(10L);
        assertThat(etat.tempsRestantMs()).isNull();
        assertThat(doubles.diffuseur.corrections).singleElement()
                .satisfies(c -> assertThat(c.repartition()).hasSize(2));
        assertThat(doubles.diffuseur.resultats).hasSize(2).first().satisfies(r -> {
            assertThat(r.idParticipant()).isEqualTo(5L);
            assertThat(r.points()).isEqualTo(900);
            assertThat(r.rang()).isEqualTo(1);
            assertThat(r.nombreParticipants()).isEqualTo(2);
            assertThat(r.repondu()).isTrue();
            assertThat(r.bonneReponse()).containsExactly("LDAP");
        });
        assertThatThrownBy(() -> doubles.clore().executer(id, ID_ENSEIGNANT))
                .isInstanceOf(AucuneQuestionOuverteException.class);
    }

    @Test
    void les_ecrans_immediats_partent_a_la_cloture_et_les_autres_sont_programmes() {
        Long id = lancer(quiz(qcm(1, 3, new EcranIntermediaire(TypeEcran.BONNE_REPONSE, 0, 0),
                new EcranIntermediaire(TypeEcran.CLASSEMENT, 1, 7))));
        doubles.demarrer().executer(id, ID_ENSEIGNANT);

        doubles.clore().executer(id, ID_ENSEIGNANT);

        assertThat(doubles.diffuseur.ecrans).singleElement()
                .satisfies(e -> assertThat(e.ecran().type()).isEqualTo(TypeEcran.BONNE_REPONSE));
        assertThat(doubles.minuterie.taches)
                .contains(new Tache("ecran-CLASSEMENT", id, 1, doubles.horloge.maintenant().plusSeconds(7)))
                .noneMatch(t -> t.type().equals("passage"));
    }

    @Test
    void un_ecran_programme_est_abandonne_si_la_seance_a_avance() {
        EcranIntermediaire ecran = new EcranIntermediaire(TypeEcran.CLASSEMENT, 0, 7);
        Long id = lancer(quiz(qcm(1, 3, ecran), qcm(2, 5)));
        doubles.demarrer().executer(id, ID_ENSEIGNANT);
        doubles.clore().executer(id, ID_ENSEIGNANT);

        doubles.clore().afficherEcran(id, 1, ecran);
        doubles.demarrer().executer(id, ID_ENSEIGNANT);
        doubles.clore().afficherEcran(id, 1, ecran);

        assertThat(doubles.diffuseur.ecrans).hasSize(1);
    }

    @Test
    void la_cloture_automatique_attend_l_echeance_du_serveur() {
        Long id = lancer(quiz(qcm(1, 3)));
        doubles.demarrer().executer(id, ID_ENSEIGNANT);
        doubles.minuterie.taches.clear();

        doubles.horloge.avancer(Duration.ofSeconds(19));
        doubles.clore().executerAEcheance(id, 1);

        assertThat(seance(id).aUneQuestionOuverte()).isTrue();
        assertThat(doubles.minuterie.taches).extracting(Tache::type).containsExactly("cloture");

        doubles.horloge.avancer(Duration.ofSeconds(1));
        doubles.clore().executerAEcheance(id, 1);

        assertThat(seance(id).aUneQuestionOuverte()).isFalse();
        assertThat(doubles.diffuseur.corrections).hasSize(1);
    }

    @Test
    void une_cloture_automatique_en_retard_ne_touche_pas_la_question_suivante() {
        Long id = lancer(quiz(qcm(1, 3), qcm(2, 5)));
        doubles.demarrer().executer(id, ID_ENSEIGNANT);
        doubles.clore().executer(id, ID_ENSEIGNANT);
        doubles.demarrer().executer(id, ID_ENSEIGNANT);
        doubles.horloge.avancer(Duration.ofMinutes(1));

        doubles.clore().executerAEcheance(id, 1);

        assertThat(seance(id).aUneQuestionOuverte()).isTrue();
    }

    @Test
    void le_passage_automatique_ouvre_la_suivante_puis_termine_apres_la_derniere() {
        Long id = lancer(quiz(false, true, 3, qcm(1, 3), qcm(2, 5)));
        doubles.demarrer().executer(id, ID_ENSEIGNANT);
        doubles.clore().executer(id, ID_ENSEIGNANT);
        assertThat(doubles.minuterie.taches)
                .contains(new Tache("passage", id, 1, doubles.horloge.maintenant().plusSeconds(3)));

        doubles.demarrer().executerAutomatiquement(id, 1);
        assertThat(seance(id).positionCourante()).isEqualTo(2);

        doubles.clore().executer(id, ID_ENSEIGNANT);
        doubles.demarrer().executerAutomatiquement(id, 2);
        assertThat(seance(id).etat()).isEqualTo(EtatSession.CLOTUREE);
    }

    @Test
    void un_passage_automatique_en_retard_est_sans_effet() {
        Long id = lancer(quiz(false, true, 3, qcm(1, 3), qcm(2, 5), qcm(3, 7)));
        doubles.demarrer().executer(id, ID_ENSEIGNANT);
        doubles.clore().executer(id, ID_ENSEIGNANT);
        doubles.demarrer().executer(id, ID_ENSEIGNANT);

        doubles.demarrer().executerAutomatiquement(id, 1);
        doubles.clore().executer(id, ID_ENSEIGNANT);
        doubles.demarrer().executerAutomatiquement(id, 1);

        assertThat(seance(id).positionCourante()).isEqualTo(2);
    }

    @Test
    void suivant_apres_la_derniere_question_termine_la_seance() {
        Long id = lancer(quiz(qcm(1, 3)));
        doubles.demarrer().executer(id, ID_ENSEIGNANT);
        doubles.clore().executer(id, ID_ENSEIGNANT);

        DeroulementSeance etat = doubles.demarrer().executer(id, ID_ENSEIGNANT);

        assertThat(etat.seance().etat()).isEqualTo(EtatSession.CLOTUREE);
        assertThat(etat.lienAcces()).as("le lien ne vaut plus").isNull();
        assertThat(etat.qrCode()).isNull();
        assertThat(doubles.diffuseur.fins).containsExactly(1);
        assertThatThrownBy(() -> doubles.demarrer().executer(id, ID_ENSEIGNANT))
                .isInstanceOf(SeanceTermineeException.class);
    }

    @Test
    void clore_le_quiz_ferme_d_abord_la_question_ouverte() {
        Long id = lancer(quiz(qcm(1, 3), qcm(2, 5)));
        doubles.demarrer().executer(id, ID_ENSEIGNANT);

        DeroulementSeance etat = doubles.cloturer().executer(id, ID_ENSEIGNANT);

        assertThat(etat.seance().etat()).isEqualTo(EtatSession.CLOTUREE);
        assertThat(doubles.appels).containsSubsequence("question", "correction", "fin");
        assertThatThrownBy(() -> doubles.cloturer().executer(id, ID_ENSEIGNANT))
                .isInstanceOf(SeanceTermineeException.class);
        assertThatThrownBy(() -> doubles.clore().executer(id, ID_ENSEIGNANT))
                .isInstanceOf(SeanceTermineeException.class);
    }

    @Test
    void une_question_devenue_incomplete_n_est_pas_ouverte() {
        Long id = lancer(quiz(qcm(1, 3), qcm(2, 5)));
        doubles.demarrer().executer(id, ID_ENSEIGNANT);
        doubles.clore().executer(id, ID_ENSEIGNANT);
        doubles.quizzes.ajouter(quiz(qcm(1, 3), incomplete(2, 5)));

        assertThatThrownBy(() -> doubles.demarrer().executer(id, ID_ENSEIGNANT))
                .isInstanceOf(QuizIncompletException.class)
                .hasMessageContaining("page 5");
        assertThat(seance(id).positionCourante()).isEqualTo(1);
    }

    @Test
    void chaque_commande_verifie_la_seance_et_son_proprietaire() {
        Long id = lancer(quiz(qcm(1, 3)));

        assertThatThrownBy(() -> doubles.demarrer().executer(id, 99L)).isInstanceOf(AccesSeanceRefuseException.class);
        assertThatThrownBy(() -> doubles.clore().executer(id, 99L)).isInstanceOf(AccesSeanceRefuseException.class);
        assertThatThrownBy(() -> doubles.cloturer().executer(id, 99L)).isInstanceOf(AccesSeanceRefuseException.class);
        assertThatThrownBy(() -> doubles.consulter().executer(id, 99L))
                .isInstanceOf(AccesSeanceRefuseException.class);
        assertThatThrownBy(() -> doubles.consulter().executer(404L, ID_ENSEIGNANT))
                .isInstanceOf(SeanceIntrouvableException.class);
        assertThatThrownBy(() -> doubles.consulter().executerPourSeanceOuverte(ID_ENSEIGNANT))
                .isInstanceOf(SeanceIntrouvableException.class)
                .hasMessage("Aucune séance n'est ouverte.");
        assertThat(seance(id).etat()).isEqualTo(EtatSession.PREPAREE);
    }

    @Test
    void les_clotures_en_attente_sont_reprogrammees_au_demarrage() {
        Long id = lancer(quiz(qcm(1, 3)));
        doubles.demarrer().executer(id, ID_ENSEIGNANT);
        doubles.minuterie.taches.clear();

        new ReprendreEcheancesUseCase(doubles.seances, doubles.minuterie, doubles.journal).executer();

        assertThat(doubles.minuterie.taches)
                .containsExactly(new Tache("cloture", id, 1, seance(id).echeanceQuestion()));
    }

    private Long lancer(Quiz quiz) {
        doubles.quizzes.ajouter(quiz);
        return doubles.lancer().executer(new LancerQuizDto(ID_QUIZ, ID_ENSEIGNANT, false)).seance().idSession();
    }

    private SeanceAnimee seance(Long idSession) {
        return doubles.seances.parId.get(idSession);
    }
}
