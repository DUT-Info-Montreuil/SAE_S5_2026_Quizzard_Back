package fr.iutmontreuil.quizzard.features.seance.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PseudonymeTest {

    @Test
    void les_espaces_de_debut_et_de_fin_sont_retires() {
        assertThat(Pseudonyme.de("  Alice  ").valeur()).isEqualTo("Alice");
    }

    @Test
    void les_espaces_multiples_sont_reduits_a_un_seul() {
        assertThat(Pseudonyme.de("Jean   Pierre").valeur()).isEqualTo("Jean Pierre");
    }

    @Test
    void une_espace_insecable_compte_comme_une_espace() {
        assertThat(Pseudonyme.de("  Alice  Bob  ").valeur()).isEqualTo("Alice Bob");
    }

    @Test
    void le_texte_est_normalise_en_nfc() {
        String decompose = "Zoé";
        String compose = "Zoé";

        assertThat(Pseudonyme.de(decompose).valeur()).isEqualTo(compose);
    }

    @Test
    void la_casse_est_conservee() {
        assertThat(Pseudonyme.de("AliCe").valeur()).isEqualTo("AliCe");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "A", " A ", "  "})
    void un_pseudonyme_absent_vide_ou_trop_court_est_refuse(String texte) {
        assertThatThrownBy(() -> Pseudonyme.de(texte)).isInstanceOf(PseudonymeInvalideException.class);
    }

    @Test
    void un_pseudonyme_de_21_caracteres_est_refuse() {
        assertThatThrownBy(() -> Pseudonyme.de("A".repeat(21))).isInstanceOf(PseudonymeInvalideException.class);
    }

    @Test
    void les_bornes_2_et_20_caracteres_sont_acceptees() {
        assertThat(Pseudonyme.de("Al").valeur()).hasSize(2);
        assertThat(Pseudonyme.de("A".repeat(20)).valeur()).hasSize(20);
    }

    @Test
    void la_longueur_compte_des_caracteres_unicode_et_non_des_unites_utf16() {
        String vingtEmojis = "😀".repeat(20);

        assertThat(Pseudonyme.de(vingtEmojis).valeur()).isEqualTo(vingtEmojis);
        assertThatThrownBy(() -> Pseudonyme.de(vingtEmojis + "😀"))
                .isInstanceOf(PseudonymeInvalideException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Al\tice", "Al\nice", "Al\u0000ice", "Al\u007Fice", "Al\u0085ice"})
    void un_caractere_de_controle_est_refuse(String texte) {
        assertThatThrownBy(() -> Pseudonyme.de(texte)).isInstanceOf(PseudonymeInvalideException.class);
    }

    @Test
    void le_message_de_refus_ne_recopie_pas_le_pseudonyme() {
        assertThatThrownBy(() -> Pseudonyme.de("x"))
                .hasMessage("Ton pseudonyme doit faire de 2 à 20 caractères.")
                .satisfies(e -> assertThat(((PseudonymeInvalideException) e).champ()).isEqualTo("pseudonyme"));
    }

    @Test
    void un_pseudonyme_non_nettoye_ne_peut_pas_etre_construit_directement() {
        assertThatThrownBy(() -> new Pseudonyme(" Alice")).isInstanceOf(PseudonymeInvalideException.class);
        assertThatThrownBy(() -> new Pseudonyme("Al  ice")).isInstanceOf(PseudonymeInvalideException.class);
        assertThatThrownBy(() -> new Pseudonyme(null)).isInstanceOf(PseudonymeInvalideException.class);
    }

    @Test
    void alice_et_alice_en_minuscules_sont_deux_pseudonymes_differents() {
        assertThat(Pseudonyme.de("Alice")).isNotEqualTo(Pseudonyme.de("alice"));
    }

    @Test
    void la_proposition_ajoute_2_a_la_fin() {
        assertThat(Pseudonyme.de("Alice").proposerAlternative(Set.of("Alice")).valeur()).isEqualTo("Alice2");
    }

    @Test
    void la_proposition_saute_les_pseudonymes_deja_pris() {
        Set<String> pris = Set.of("Alice", "Alice2", "Alice3");

        assertThat(Pseudonyme.de("Alice").proposerAlternative(pris).valeur()).isEqualTo("Alice4");
    }

    @Test
    void la_proposition_raccourcit_la_racine_pour_rester_a_20_caracteres() {
        Pseudonyme vingt = Pseudonyme.de("ABCDEFGHIJKLMNOPQRST");

        assertThat(vingt.proposerAlternative(Set.of(vingt.valeur())).valeur()).isEqualTo("ABCDEFGHIJKLMNOPQRS2");
        assertThat(vingt.proposerAlternative(Set.of(vingt.valeur(), "ABCDEFGHIJKLMNOPQRS2")).valeur())
                .isEqualTo("ABCDEFGHIJKLMNOPQRS3");
    }

    @Test
    void la_proposition_reste_a_20_caracteres_avec_un_suffixe_a_deux_chiffres() {
        Pseudonyme vingt = Pseudonyme.de("ABCDEFGHIJKLMNOPQRST");
        Set<String> pris = Set.of(vingt.valeur(), "ABCDEFGHIJKLMNOPQRS2", "ABCDEFGHIJKLMNOPQRS3",
                "ABCDEFGHIJKLMNOPQRS4", "ABCDEFGHIJKLMNOPQRS5", "ABCDEFGHIJKLMNOPQRS6", "ABCDEFGHIJKLMNOPQRS7",
                "ABCDEFGHIJKLMNOPQRS8", "ABCDEFGHIJKLMNOPQRS9");

        assertThat(vingt.proposerAlternative(pris).valeur()).isEqualTo("ABCDEFGHIJKLMNOPQR10");
    }

    @Test
    void la_proposition_ne_laisse_pas_d_espace_avant_le_suffixe() {
        // 20 caractères dont une espace en 19e position : raccourcie à 19, la racine finirait par une espace.
        Pseudonyme pseudo = Pseudonyme.de("ABCDEFGHIJKLMNOPQR S");

        assertThat(pseudo.proposerAlternative(Set.of(pseudo.valeur())).valeur()).isEqualTo("ABCDEFGHIJKLMNOPQR2");
    }

    @Test
    void le_pseudonyme_ne_apparait_pas_dans_tostring() {
        assertThat(Pseudonyme.de("Alice").toString()).doesNotContain("Alice");
    }
}
