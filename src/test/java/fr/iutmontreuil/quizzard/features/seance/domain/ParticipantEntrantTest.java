package fr.iutmontreuil.quizzard.features.seance.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ParticipantEntrantTest {

    private static final String EMPREINTE = "a".repeat(64);
    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 24, 10, 30, 0);

    @Test
    void un_participant_qui_entre_a_un_score_a_zero_et_aucune_identite() {
        ParticipantEntrant participant = ParticipantEntrant.rejoindre(7L, Pseudonyme.de("Alice"), EMPREINTE, MAINTENANT);

        assertThat(participant.idParticipant()).isNull();
        assertThat(participant.idSession()).isEqualTo(7L);
        assertThat(participant.pseudonyme()).isEqualTo(Pseudonyme.de("Alice"));
        assertThat(participant.empreinteJeton()).isEqualTo(EMPREINTE);
        assertThat(participant.scoreTotal()).isZero();
        assertThat(participant.identiteVerifiee()).isFalse();
    }

    @Test
    void la_date_d_entree_et_la_derniere_activite_valent_maintenant() {
        ParticipantEntrant participant = ParticipantEntrant.rejoindre(7L, Pseudonyme.de("Alice"), EMPREINTE, MAINTENANT);

        assertThat(participant.dateEntree()).isEqualTo(MAINTENANT);
        assertThat(participant.derniereActivite()).isEqualTo(MAINTENANT);
    }

    static Stream<String> empreintesInvalides() {
        return Stream.of(null, "", "abc", "g".repeat(64), "A".repeat(64), "a".repeat(63), "a".repeat(65));
    }

    @ParameterizedTest
    @MethodSource("empreintesInvalides")
    void une_empreinte_qui_n_a_pas_64_caracteres_hexadecimaux_minuscules_est_une_erreur_de_programmation(
            String empreinte) {
        assertThatThrownBy(() -> ParticipantEntrant.rejoindre(7L, Pseudonyme.de("Alice"), empreinte, MAINTENANT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void l_enregistrement_attribue_un_identifiant_sans_changer_le_reste() {
        ParticipantEntrant participant = ParticipantEntrant.rejoindre(7L, Pseudonyme.de("Alice"), EMPREINTE, MAINTENANT);

        ParticipantEntrant enregistre = participant.avecIdentifiant(42L);

        assertThat(enregistre.idParticipant()).isEqualTo(42L);
        assertThat(enregistre.idSession()).isEqualTo(7L);
        assertThat(enregistre.pseudonyme()).isEqualTo(participant.pseudonyme());
        assertThat(enregistre.empreinteJeton()).isEqualTo(EMPREINTE);
        assertThat(enregistre.dateEntree()).isEqualTo(MAINTENANT);
        assertThat(participant.idParticipant()).isNull();
    }

    @Test
    void ni_le_pseudonyme_ni_l_empreinte_n_apparaissent_dans_tostring() {
        ParticipantEntrant participant = ParticipantEntrant.rejoindre(7L, Pseudonyme.de("Alice"), EMPREINTE, MAINTENANT);

        assertThat(participant.toString()).doesNotContain("Alice").doesNotContain(EMPREINTE);
    }
}
