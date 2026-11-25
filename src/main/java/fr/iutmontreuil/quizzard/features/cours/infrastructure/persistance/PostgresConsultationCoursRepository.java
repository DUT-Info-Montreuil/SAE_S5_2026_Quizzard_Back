package fr.iutmontreuil.quizzard.features.cours.infrastructure.persistance;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.iutmontreuil.quizzard.features.cours.application.dto.CoursConsulte;
import fr.iutmontreuil.quizzard.features.cours.application.port.ConsultationCoursRepository;

/**
 * Adaptateur du port {@link ConsultationCoursRepository}, en SQL : une seule requête donne chaque cours avec sa
 * dernière version et son nombre de quiz, quel que soit le nombre de cours.
 */
@Repository
class PostgresConsultationCoursRepository implements ConsultationCoursRepository {

    /** Cours non archivés d'un enseignant ; un cours a toujours au moins une version (UC2). */
    private static final String COURS_DE_L_ENSEIGNANT = """
            SELECT c.id_cours, c.titre, c.date_modification, v.id_version, v.numero, v.nb_pages,
                   (SELECT count(*) FROM quiz q WHERE q.id_cours = c.id_cours AND NOT q.archive) AS nb_quiz
              FROM cours c
              JOIN LATERAL (SELECT id_version, numero, nb_pages FROM version_cours
                             WHERE id_cours = c.id_cours ORDER BY numero DESC LIMIT 1) v ON TRUE
             WHERE c.id_enseignant = :idEnseignant AND NOT c.archive
            """;

    private final JdbcClient jdbc;

    PostgresConsultationCoursRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CoursConsulte> listerParEnseignant(Long idEnseignant) {
        return jdbc.sql(COURS_DE_L_ENSEIGNANT + " ORDER BY c.date_modification DESC, c.id_cours DESC")
                .param("idEnseignant", idEnseignant)
                .query((ligne, i) -> versCours(ligne))
                .list();
    }

    @Override
    public Optional<CoursConsulte> trouverParEnseignant(Long idCours, Long idEnseignant) {
        return jdbc.sql(COURS_DE_L_ENSEIGNANT + " AND c.id_cours = :idCours")
                .param("idEnseignant", idEnseignant)
                .param("idCours", idCours)
                .query((ligne, i) -> versCours(ligne))
                .optional();
    }

    private static CoursConsulte versCours(ResultSet ligne) throws SQLException {
        return new CoursConsulte(ligne.getLong("id_cours"), ligne.getString("titre"), ligne.getLong("id_version"),
                ligne.getInt("numero"), ligne.getInt("nb_pages"), ligne.getInt("nb_quiz"),
                ligne.getObject("date_modification", LocalDateTime.class));
    }
}
