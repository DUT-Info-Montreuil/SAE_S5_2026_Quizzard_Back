package fr.iutmontreuil.quizzard.features.seance.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** UC8 : une séance accepte une entrée sans compte si elle n'est pas close et si le quiz n'est pas noté. */
class ConditionsEntreeTest {

    private static SessionQuiz session(EtatSession etat, boolean estNotee) {
        return new SessionQuiz(1L, "code-secret", etat, estNotee, 0, null, null);
    }

    @ParameterizedTest
    @EnumSource(value = EtatSession.class, names = {"PREPAREE", "EN_COURS"})
    void une_seance_preparee_ou_en_cours_est_ouverte(EtatSession etat) {
        assertThatCode(() -> ConditionsEntree.verifierSeanceOuverte(session(etat, false)))
                .doesNotThrowAnyException();
    }

    @Test
    void une_seance_cloturee_n_est_plus_ouverte() {
        assertThatThrownBy(() -> ConditionsEntree.verifierSeanceOuverte(session(EtatSession.CLOTUREE, false)))
                .isInstanceOf(SessionCloseException.class)
                .hasMessage("Ce quiz est terminé.");
    }

    @ParameterizedTest
    @EnumSource(value = EtatSession.class, names = {"PREPAREE", "EN_COURS"})
    void un_quiz_non_note_accepte_l_entree_sans_compte(EtatSession etat) {
        assertThatCode(() -> ConditionsEntree.verifierEntreeSansCompte(session(etat, false)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = EtatSession.class, names = {"PREPAREE", "EN_COURS"})
    void un_quiz_note_refuse_l_entree_sans_compte(EtatSession etat) {
        assertThatThrownBy(() -> ConditionsEntree.verifierEntreeSansCompte(session(etat, true)))
                .isInstanceOf(ConnexionIutRequiseException.class)
                .hasMessage("Ce quiz est noté : connecte-toi avec ton compte IUT.");
    }

    @Test
    void une_seance_cloturee_refuse_l_entree_sans_compte() {
        assertThatThrownBy(() -> ConditionsEntree.verifierEntreeSansCompte(session(EtatSession.CLOTUREE, false)))
                .isInstanceOf(SessionCloseException.class);
    }

    @Test
    void une_seance_cloturee_et_notee_est_refusee_comme_close() {
        assertThatThrownBy(() -> ConditionsEntree.verifierEntreeSansCompte(session(EtatSession.CLOTUREE, true)))
                .isInstanceOf(SessionCloseException.class);
    }
}
