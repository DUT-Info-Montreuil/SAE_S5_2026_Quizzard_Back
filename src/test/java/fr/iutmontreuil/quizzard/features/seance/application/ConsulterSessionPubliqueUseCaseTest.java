package fr.iutmontreuil.quizzard.features.seance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import fr.iutmontreuil.quizzard.features.seance.application.DoublesSeance.InformationsFixes;
import fr.iutmontreuil.quizzard.features.seance.application.DoublesSeance.SessionsEnMemoire;
import fr.iutmontreuil.quizzard.features.seance.application.dto.SessionPublique;
import fr.iutmontreuil.quizzard.features.seance.domain.EtatSession;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionCloseException;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionIntrouvableException;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionQuiz;

class ConsulterSessionPubliqueUseCaseTest {

    private static final String CODE = "code-de-la-seance";

    private static ConsulterSessionPubliqueUseCase casUsage(SessionQuiz session) {
        return new ConsulterSessionPubliqueUseCase(new SessionsEnMemoire().avec(session), new InformationsFixes());
    }

    @ParameterizedTest
    @EnumSource(value = EtatSession.class, names = {"PREPAREE", "EN_COURS"})
    void une_seance_ouverte_renvoie_les_titres_le_mode_note_et_l_etat(EtatSession etat) {
        SessionPublique resultat = casUsage(new SessionQuiz(5L, CODE, etat, false, 0, null, null)).executer(CODE);

        assertThat(resultat.titreCours()).isEqualTo("Réseaux");
        assertThat(resultat.titreQuiz()).isEqualTo("Quiz du chapitre 3");
        assertThat(resultat.estNotee()).isFalse();
        assertThat(resultat.etat()).isEqualTo(etat);
    }

    @Test
    void un_quiz_note_est_consultable_pour_que_le_front_propose_la_connexion_iut() {
        SessionPublique resultat = casUsage(new SessionQuiz(5L, CODE, EtatSession.PREPAREE, true, 0, null, null))
                .executer(CODE);

        assertThat(resultat.estNotee()).isTrue();
    }

    @Test
    void un_code_d_acces_inconnu_est_introuvable() {
        assertThatThrownBy(() -> casUsage(new SessionQuiz(5L, CODE, EtatSession.PREPAREE, false, 0, null, null))
                .executer("inconnu"))
                .isInstanceOf(SessionIntrouvableException.class);
    }

    @Test
    void une_seance_cloturee_est_refusee() {
        assertThatThrownBy(() -> casUsage(new SessionQuiz(5L, CODE, EtatSession.CLOTUREE, false, 0, null, null))
                .executer(CODE))
                .isInstanceOf(SessionCloseException.class);
    }
}
