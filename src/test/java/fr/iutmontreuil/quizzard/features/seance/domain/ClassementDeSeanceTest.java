package fr.iutmontreuil.quizzard.features.seance.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import fr.iutmontreuil.quizzard.features.seance.domain.ClassementDeSeance.Rang;

class ClassementDeSeanceTest {

    private static final LocalDateTime ARRIVEE = LocalDateTime.of(2026, 11, 25, 14, 0);

    @Test
    void le_score_le_plus_haut_passe_devant() {
        Map<Long, Rang> rangs = ClassementDeSeance.apresQuestion(
                List.of(score(1, 500, 4000, 0), score(2, 900, 6000, 0)), true);

        assertThat(rangs.get(2L).rang()).isEqualTo(1);
        assertThat(rangs.get(1L).rang()).isEqualTo(2);
    }

    @Test
    void a_egalite_le_temps_cumule_le_plus_faible_puis_l_ordre_d_arrivee_departagent() {
        Map<Long, Rang> rangs = ClassementDeSeance.apresQuestion(
                List.of(score(1, 500, 4000, 2), score(2, 500, 3000, 3), score(3, 500, 4000, 1)), true);

        assertThat(rangs.get(2L).rang()).isEqualTo(1);
        assertThat(rangs.get(3L).rang()).isEqualTo(2);
        assertThat(rangs.get(1L).rang()).isEqualTo(3);
    }

    @Test
    void la_premiere_question_n_a_pas_d_evolution() {
        Map<Long, Rang> rangs = ClassementDeSeance.apresQuestion(
                List.of(score(1, 0, 0, 0), score(2, 900, 1000, 1)), true);

        assertThat(rangs.values()).extracting(Rang::evolution).containsOnly(0);
    }

    @Test
    void l_evolution_compte_les_places_gagnees_depuis_la_question_precedente() {
        // Avant la question : 1 (800 points) devant 2 (500). Le 2 gagne 600 points et passe devant.
        Map<Long, Rang> rangs = ClassementDeSeance.apresQuestion(List.of(
                new ScoreParticipant(1L, 800, 3000, ARRIVEE, false, 0, 0),
                new ScoreParticipant(2L, 1100, 5000, ARRIVEE.plusSeconds(1), true, 600, 2000)), false);

        assertThat(rangs.get(2L)).isEqualTo(new Rang(1, 1));
        assertThat(rangs.get(1L)).isEqualTo(new Rang(2, -1));
    }

    @Test
    void une_seance_sans_participant_n_a_pas_de_rang() {
        assertThat(ClassementDeSeance.apresQuestion(List.of(), false)).isEmpty();
    }

    private static ScoreParticipant score(long id, int total, long temps, int ordreArrivee) {
        return new ScoreParticipant(id, total, temps, ARRIVEE.plusSeconds(ordreArrivee), false, 0, 0);
    }
}
