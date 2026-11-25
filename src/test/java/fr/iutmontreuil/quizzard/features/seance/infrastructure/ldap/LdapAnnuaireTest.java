package fr.iutmontreuil.quizzard.features.seance.infrastructure.ldap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Cas d'indisponibilité seulement : aucun annuaire n'est disponible pendant les tests. */
class LdapAnnuaireTest {

    @Test
    void sans_adresse_configuree_l_annuaire_est_indisponible() {
        assertThat(new LdapAnnuaire("  ").estDisponible()).isFalse();
    }

    @Test
    void une_liaison_non_chiffree_est_refusee() {
        assertThat(new LdapAnnuaire("ldap://127.0.0.1:389").estDisponible()).isFalse();
    }

    @Test
    void un_annuaire_qui_ne_repond_pas_est_indisponible_sans_exception() {
        // Le port 1 n'écoute pas.
        assertThat(new LdapAnnuaire("ldaps://127.0.0.1:1").estDisponible()).isFalse();
    }
}
