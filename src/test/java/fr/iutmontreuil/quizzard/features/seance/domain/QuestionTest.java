package fr.iutmontreuil.quizzard.features.seance.domain;

import static fr.iutmontreuil.quizzard.features.seance.domain.Fabrique.cochees;
import static fr.iutmontreuil.quizzard.features.seance.domain.Fabrique.liens;
import static fr.iutmontreuil.quizzard.features.seance.domain.Fabrique.repondre;
import static fr.iutmontreuil.quizzard.features.seance.domain.Fabrique.texte;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Part juste et points selon le type de question (DESIGN.md §3.3 et §3.4). */
class QuestionTest {

    private static final Bareme TOUT_OU_RIEN = new Bareme(1000, true, false);
    private static final Bareme PARTIELS = new Bareme(1000, true, true);

    @Test
    void un_qcm_juste_en_0_seconde_vaut_1000() {
        QuestionQcm qcm = Fabrique.qcmUneBonne(TOUT_OU_RIEN);
        assertThat(qcm.evaluer(repondre(qcm, cochees(41L)), 0)).isEqualTo(1000);
    }

    @Test
    void un_qcm_faux_vaut_0() {
        QuestionQcm qcm = Fabrique.qcmUneBonne(TOUT_OU_RIEN);
        assertThat(qcm.evaluer(repondre(qcm, cochees(42L)), 0)).isZero();
    }

    @Test
    void un_qcm_a_trois_bonnes_avec_deux_bonnes_et_une_mauvaise_vaut_333_avec_points_partiels() {
        QuestionQcm qcm = Fabrique.qcmTroisBonnes(PARTIELS);
        assertThat(qcm.evaluer(repondre(qcm, cochees(51L, 52L, 54L)), 0)).isEqualTo(333);
    }

    @Test
    void un_qcm_incomplet_sans_points_partiels_vaut_0() {
        QuestionQcm qcm = Fabrique.qcmTroisBonnes(TOUT_OU_RIEN);
        assertThat(qcm.evaluer(repondre(qcm, cochees(51L, 52L)), 0)).isZero();
    }

    @Test
    void les_points_partiels_d_un_qcm_ne_sont_jamais_negatifs() {
        QuestionQcm qcm = Fabrique.qcmTroisBonnes(PARTIELS);
        assertThat(qcm.evaluer(repondre(qcm, cochees(54L)), 0)).isZero();
    }

    @Test
    void le_choix_multiple_se_deduit_du_nombre_de_bonnes_reponses() {
        assertThat(Fabrique.qcmUneBonne(TOUT_OU_RIEN).choixMultiple()).isFalse();
        assertThat(Fabrique.qcmTroisBonnes(TOUT_OU_RIEN).choixMultiple()).isTrue();
    }

    @Test
    void relier_3_paires_justes_sur_4_vaut_750_avec_points_partiels() {
        QuestionPointsARelier relier = Fabrique.relierQuatre(PARTIELS);
        Reponse reponse = repondre(relier, liens(new Lien(61L, 61L), new Lien(62L, 62L), new Lien(63L, 63L)));
        assertThat(relier.evaluer(reponse, 0)).isEqualTo(750);
    }

    @Test
    void relier_3_paires_justes_sur_4_vaut_0_sans_points_partiels() {
        QuestionPointsARelier relier = Fabrique.relierQuatre(TOUT_OU_RIEN);
        Reponse reponse = repondre(relier, liens(new Lien(61L, 61L), new Lien(62L, 62L), new Lien(63L, 63L)));
        assertThat(relier.evaluer(reponse, 0)).isZero();
    }

    @Test
    void relier_deux_paires_croisees_ne_rapporte_que_les_liens_justes() {
        QuestionPointsARelier relier = Fabrique.relierQuatre(PARTIELS);
        Reponse reponse = repondre(relier, liens(new Lien(61L, 61L), new Lien(62L, 62L), new Lien(63L, 64L),
                new Lien(64L, 63L)));
        assertThat(relier.evaluer(reponse, 0)).isEqualTo(500);
    }

    @Test
    void une_reponse_libre_acceptee_par_un_motif_vaut_tous_les_points() {
        QuestionReponseLibre libre = Fabrique.libre(TOUT_OU_RIEN);
        Reponse reponse = repondre(libre, texte("  LDAP "));
        assertThat(libre.evaluer(reponse, 0)).isEqualTo(1000);
        assertThat(libre.statutCorrection(reponse)).isEqualTo(StatutCorrection.ACCEPTEE_PAR_MOTIF);
    }

    @Test
    void une_reponse_libre_sans_motif_correspondant_est_a_examiner_et_vaut_0() {
        QuestionReponseLibre libre = Fabrique.libre(TOUT_OU_RIEN);
        Reponse reponse = repondre(libre, texte("smtp"));
        assertThat(libre.evaluer(reponse, 0)).isZero();
        assertThat(libre.statutCorrection(reponse)).isEqualTo(StatutCorrection.A_EXAMINER);
    }

    @Test
    void un_qcm_a_le_statut_sans_objet() {
        QuestionQcm qcm = Fabrique.qcmUneBonne(TOUT_OU_RIEN);
        assertThat(qcm.statutCorrection(repondre(qcm, cochees(41L)))).isEqualTo(StatutCorrection.SANS_OBJET);
    }
}
