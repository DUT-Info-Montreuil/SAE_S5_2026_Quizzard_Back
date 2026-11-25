package fr.iutmontreuil.quizzard.features.cours;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Enseignants de test, que l'application ne sait pas encore créer. {@link #nettoyer()} supprime ceux qui ont été
 * créés, avec les cours qu'ils ont importés.
 */
public class FixturesImport {

    /** Un enseignant et son identifiant de connexion. */
    public record Enseignant(Long id, String identifiant) {
    }

    private final JdbcClient jdbc;
    private final List<Long> enseignants = new ArrayList<>();

    public FixturesImport(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Enseignant creerEnseignant() {
        return creerEnseignant("ens-" + UUID.randomUUID());
    }

    public Enseignant creerEnseignant(String identifiant) {
        Long id = jdbc.sql("""
                        INSERT INTO enseignant (identifiant_ldap, nom, prenom, origine_compte, role, date_creation)
                        VALUES (:identifiant, 'Nom', 'Prenom', 'LDAP', 'ENSEIGNANT', :maintenant)
                        RETURNING id_enseignant
                        """)
                .param("identifiant", identifiant)
                .param("maintenant", LocalDateTime.now())
                .query(Long.class).single();
        enseignants.add(id);
        return new Enseignant(id, identifiant);
    }

    public int nombreDeCours(Enseignant enseignant) {
        return jdbc.sql("SELECT count(*) FROM cours WHERE id_enseignant = :id").param("id", enseignant.id())
                .query(Integer.class).single();
    }

    public void nettoyer() {
        for (Long id : enseignants) {
            jdbc.sql("DELETE FROM version_cours WHERE id_cours IN "
                    + "(SELECT id_cours FROM cours WHERE id_enseignant = :id)").param("id", id).update();
            jdbc.sql("DELETE FROM cours WHERE id_enseignant = :id").param("id", id).update();
            jdbc.sql("DELETE FROM enseignant WHERE id_enseignant = :id").param("id", id).update();
        }
        enseignants.clear();
    }
}
