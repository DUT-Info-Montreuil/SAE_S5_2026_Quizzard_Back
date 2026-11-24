package fr.iutmontreuil.quizzard.features.seance.domain;

import static fr.iutmontreuil.quizzard.features.seance.domain.Fabrique.cochees;
import static fr.iutmontreuil.quizzard.features.seance.domain.Fabrique.liens;
import static fr.iutmontreuil.quizzard.features.seance.domain.Fabrique.repondre;
import static fr.iutmontreuil.quizzard.features.seance.domain.Fabrique.texte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Fabrication et validation du contenu d'une réponse : ReponseInvalideException (422) pour tout contenu incohérent. */
class ReponseTest {

    private static final Bareme BAREME = new Bareme(1000, true, false);

    @Test
    void le_contenu_absent_pour_le_type_de_la_question_est_refuse() {
        assertThatThrownBy(() -> repondre(Fabrique.qcmUneBonne(BAREME), texte("LDAP")))
                .isInstanceOf(ReponseInvalideException.class);
        assertThatThrownBy(() -> repondre(Fabrique.relierQuatre(BAREME), cochees(41L)))
                .isInstanceOf(ReponseInvalideException.class);
        assertThatThrownBy(() -> repondre(Fabrique.libre(BAREME), cochees(41L)))
                .isInstanceOf(ReponseInvalideException.class);
    }

    @Test
    void un_qcm_sans_proposition_cochee_est_refuse() {
        assertThatThrownBy(() -> repondre(Fabrique.qcmUneBonne(BAREME), new ContenuReponse(List.of(), null, null)))
                .isInstanceOf(ReponseInvalideException.class);
    }

    @Test
    void une_proposition_d_une_autre_question_est_refusee() {
        assertThatThrownBy(() -> repondre(Fabrique.qcmUneBonne(BAREME), cochees(51L)))
                .isInstanceOf(ReponseInvalideException.class);
    }

    @Test
    void une_proposition_cochee_deux_fois_est_refusee() {
        assertThatThrownBy(() -> repondre(Fabrique.qcmTroisBonnes(BAREME), cochees(51L, 51L)))
                .isInstanceOf(ReponseInvalideException.class);
    }

    @Test
    void plusieurs_choix_sur_un_qcm_a_une_seule_bonne_reponse_sont_refuses() {
        assertThatThrownBy(() -> repondre(Fabrique.qcmUneBonne(BAREME), cochees(41L, 42L)))
                .isInstanceOf(ReponseInvalideException.class);
    }

    @Test
    void un_element_relie_deux_fois_est_refuse() {
        assertThatThrownBy(() -> repondre(Fabrique.relierQuatre(BAREME),
                liens(new Lien(61L, 61L), new Lien(61L, 62L))))
                .isInstanceOf(ReponseInvalideException.class);
        assertThatThrownBy(() -> repondre(Fabrique.relierQuatre(BAREME),
                liens(new Lien(61L, 61L), new Lien(62L, 61L))))
                .isInstanceOf(ReponseInvalideException.class);
    }

    @Test
    void un_element_d_une_autre_question_est_refuse() {
        assertThatThrownBy(() -> repondre(Fabrique.relierQuatre(BAREME), liens(new Lien(61L, 99L))))
                .isInstanceOf(ReponseInvalideException.class);
    }

    @Test
    void un_texte_vide_ou_trop_long_est_refuse() {
        assertThatThrownBy(() -> repondre(Fabrique.libre(BAREME), texte("   ")))
                .isInstanceOf(ReponseInvalideException.class);
        assertThatThrownBy(() -> repondre(Fabrique.libre(BAREME), texte("a".repeat(1001))))
                .isInstanceOf(ReponseInvalideException.class);
    }

    @Test
    void un_texte_de_1000_caracteres_est_accepte() {
        assertThat(repondre(Fabrique.libre(BAREME), texte("a".repeat(1000)))).isInstanceOf(ReponseLibre.class);
    }

    @Test
    void la_reponse_est_horodatee_par_le_serveur_et_garde_le_participant() {
        Reponse reponse = Reponse.creer(Fabrique.qcmUneBonne(BAREME), cochees(41L), 7L, Fabrique.MAINTENANT, 1234);
        assertThat(reponse.horodatageServeur()).isEqualTo(Fabrique.MAINTENANT);
        assertThat(reponse.tempsReponseMs()).isEqualTo(1234);
        assertThat(reponse.idParticipant()).isEqualTo(7L);
        assertThat(reponse.idQuestion()).isEqualTo(1L);
    }

    @Test
    void la_normalisation_reduit_les_espaces_et_passe_en_minuscules_en_gardant_les_accents() {
        assertThat(TexteReponse.normaliser("  Protocole   LDAP\t Été ")).isEqualTo("protocole ldap été");
    }
}
