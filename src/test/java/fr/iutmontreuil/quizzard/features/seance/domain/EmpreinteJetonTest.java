package fr.iutmontreuil.quizzard.features.seance.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** L'empreinte enregistrée à l'entrée (UC8) doit être celle que les autres cas d'usage recalculent. */
class EmpreinteJetonTest {

    @Test
    void l_empreinte_est_le_sha256_en_hexadecimal_minuscule() {
        // Vecteur de test publié du SHA-256 : « abc ».
        assertThat(JetonParticipant.empreinte("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
