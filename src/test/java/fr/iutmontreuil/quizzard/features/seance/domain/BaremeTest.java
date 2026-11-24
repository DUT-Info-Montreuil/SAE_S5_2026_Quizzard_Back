package fr.iutmontreuil.quizzard.features.seance.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/** Formule des points (DESIGN.md §3.2) et exemples de §3.4 et de l'UC9. */
class BaremeTest {

    private static final long VINGT_SECONDES = 20_000;
    private final Bareme avecDecroissance = new Bareme(1000, true, false);

    @Test
    void une_reponse_juste_instantanee_vaut_tous_les_points() {
        assertThat(avecDecroissance.calculerPoints(0, VINGT_SECONDES, 1)).isEqualTo(1000);
    }

    @Test
    void une_reponse_juste_a_mi_temps_vaut_750_points() {
        assertThat(avecDecroissance.calculerPoints(10_000, VINGT_SECONDES, 1)).isEqualTo(750);
    }

    @Test
    void une_reponse_juste_a_la_derniere_milliseconde_vaut_la_moitie() {
        assertThat(avecDecroissance.calculerPoints(20_000, VINGT_SECONDES, 1)).isEqualTo(500);
    }

    @Test
    void une_reponse_juste_en_5_secondes_sur_20_vaut_875_points() {
        assertThat(avecDecroissance.calculerPoints(5_000, VINGT_SECONDES, 1)).isEqualTo(875);
    }

    @Test
    void sans_decroissance_le_temps_ne_compte_pas() {
        assertThat(new Bareme(1000, false, false).calculerPoints(15_000, VINGT_SECONDES, 1)).isEqualTo(1000);
    }

    @Test
    void le_temps_est_borne_a_la_duree() {
        assertThat(avecDecroissance.calculerPoints(99_000, VINGT_SECONDES, 1)).isEqualTo(500);
        assertThat(avecDecroissance.calculerPoints(-5, VINGT_SECONDES, 1)).isEqualTo(1000);
    }

    @Test
    void une_fraction_hors_de_0_1_est_une_erreur_de_programmation() {
        assertThatIllegalArgumentException().isThrownBy(() -> avecDecroissance.calculerPoints(0, 1000, 1.5));
    }
}
