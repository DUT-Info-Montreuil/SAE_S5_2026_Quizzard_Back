package fr.iutmontreuil.quizzard.features.cours.infrastructure.persistance;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.iutmontreuil.quizzard.features.cours.application.port.CoursRepository;
import fr.iutmontreuil.quizzard.features.cours.domain.Cours;
import fr.iutmontreuil.quizzard.features.cours.domain.VersionCours;

/** Cours en SQL (JdbcClient, comme le journal) : la ligne du cours est verrouillée pour la modification. */
@Repository
class PostgresCoursRepository implements CoursRepository {

    private final JdbcClient jdbc;

    PostgresCoursRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Cours> trouverPourModification(Long idCours, Long idEnseignant) {
        return jdbc.sql("""
                        SELECT id_cours, id_enseignant, archive FROM cours
                         WHERE id_cours = :idCours AND id_enseignant = :idEnseignant
                           FOR UPDATE
                        """)
                .param("idCours", idCours)
                .param("idEnseignant", idEnseignant)
                .query((l, i) -> new Cours(l.getLong("id_cours"), l.getLong("id_enseignant"),
                        l.getBoolean("archive"), derniereVersion(idCours).orElse(null)))
                .optional();
    }

    private Optional<VersionCours> derniereVersion(Long idCours) {
        return jdbc.sql("""
                        SELECT id_version, numero, nb_pages FROM version_cours
                         WHERE id_cours = :idCours ORDER BY numero DESC LIMIT 1
                        """)
                .param("idCours", idCours)
                .query((l, i) -> new VersionCours(l.getLong("id_version"), l.getInt("numero"), l.getInt("nb_pages")))
                .optional();
    }

    @Override
    public void marquerModifie(Long idCours, LocalDateTime maintenant) {
        jdbc.sql("UPDATE cours SET date_modification = :maintenant WHERE id_cours = :idCours")
                .param("maintenant", maintenant)
                .param("idCours", idCours)
                .update();
    }
}
