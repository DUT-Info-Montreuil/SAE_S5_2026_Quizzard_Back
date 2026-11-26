package fr.iutmontreuil.quizzard.features.cours;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Enseignants de test, que l'application ne sait pas encore créer, et leurs cours. {@link #nettoyer()} supprime les
 * enseignants créés, avec leurs cours, versions et quiz.
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

    /** Un cours non archivé sans version : {@link #ajouterVersion} lui en donne une. */
    public Long creerCours(Enseignant enseignant, String titre, LocalDateTime dateModification) {
        return jdbc.sql("""
                        INSERT INTO cours (titre, date_creation, date_modification, id_enseignant)
                        VALUES (:titre, :date, :date, :enseignant)
                        RETURNING id_cours
                        """)
                .param("titre", titre)
                .param("date", dateModification)
                .param("enseignant", enseignant.id())
                .query(Long.class).single();
    }

    /** Une version d'un cours, sans fichier sur le disque. */
    public Long ajouterVersion(Long idCours, int numero, int nbPages) {
        return jdbc.sql("""
                        INSERT INTO version_cours (numero, nom_fichier, chemin_stockage, taille_octets, nb_pages,
                                                   empreinte, date_import, id_cours)
                        VALUES (:numero, 'cours.pdf', :chemin, 1, :nbPages, :empreinte, :maintenant, :idCours)
                        RETURNING id_version
                        """)
                .param("numero", numero)
                .param("chemin", "cours/" + UUID.randomUUID() + ".pdf")
                .param("nbPages", nbPages)
                .param("empreinte", "a".repeat(64))
                .param("maintenant", LocalDateTime.now())
                .param("idCours", idCours)
                .query(Long.class).single();
    }

    public void ajouterQuiz(Long idCours, boolean archive) {
        jdbc.sql("""
                        INSERT INTO quiz (titre, duree_par_defaut_sec, affichage_cours, archive, id_cours)
                        VALUES ('Quiz', 20, 'PROJETE_SEUL', :archive, :idCours)
                        """)
                .param("archive", archive)
                .param("idCours", idCours)
                .update();
    }

    public void archiver(Long idCours) {
        jdbc.sql("UPDATE cours SET archive = TRUE WHERE id_cours = :id").param("id", idCours).update();
    }

    public void nettoyer() {
        for (Long id : enseignants) {
            jdbc.sql("DELETE FROM quiz WHERE id_cours IN "
                    + "(SELECT id_cours FROM cours WHERE id_enseignant = :id)").param("id", id).update();
            jdbc.sql("DELETE FROM version_cours WHERE id_cours IN "
                    + "(SELECT id_cours FROM cours WHERE id_enseignant = :id)").param("id", id).update();
            jdbc.sql("DELETE FROM cours WHERE id_enseignant = :id").param("id", id).update();
            jdbc.sql("DELETE FROM enseignant WHERE id_enseignant = :id").param("id", id).update();
        }
        enseignants.clear();
    }
}
