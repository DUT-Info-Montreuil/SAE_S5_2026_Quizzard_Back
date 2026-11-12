package fr.iutmontreuil.quizzard.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithMockUser;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/** Les actions sensibles sont conservées dans journal_evenement, avec l'acteur connecté. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JournalEvenementsTest {

    @Autowired
    JournalEvenements journal;

    @Autowired
    JdbcClient jdbc;

    @Test
    @WithMockUser(username = "prof.test")
    void une_action_sensible_est_ecrite_dans_la_table() {
        journal.actionSensible("cours_importe", Map.of("cible", "cours 7", "nb_pages", 12));

        Map<String, Object> ligne = jdbc.sql("""
                        SELECT acteur, type_action, cible, details FROM journal_evenement
                        WHERE type_action = 'cours_importe' ORDER BY id_evenement DESC LIMIT 1
                        """)
                .query().singleRow();
        assertThat(ligne.get("acteur")).isEqualTo("prof.test");
        assertThat(ligne.get("cible")).isEqualTo("cours 7");
        assertThat((String) ligne.get("details")).contains("\"nb_pages\":12");
    }

    @Test
    void un_nom_d_action_trop_long_est_refuse() {
        assertThatThrownBy(() -> journal.actionSensible("x".repeat(31), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
