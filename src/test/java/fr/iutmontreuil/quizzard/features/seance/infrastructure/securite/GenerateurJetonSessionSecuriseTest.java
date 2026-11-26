package fr.iutmontreuil.quizzard.features.seance.infrastructure.securite;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class GenerateurJetonSessionSecuriseTest {

    private final GenerateurJetonSessionSecurise generateur = new GenerateurJetonSessionSecurise();

    @Test
    void le_code_fait_22_caracteres_utilisables_dans_une_adresse() {
        assertThat(generateur.generer().valeur()).matches("[A-Za-z0-9_-]{22}");
    }

    @Test
    void deux_codes_ne_sont_pas_identiques() {
        assertThat(IntStream.range(0, 1000).mapToObj(i -> generateur.generer().valeur())).doesNotHaveDuplicates();
    }
}
