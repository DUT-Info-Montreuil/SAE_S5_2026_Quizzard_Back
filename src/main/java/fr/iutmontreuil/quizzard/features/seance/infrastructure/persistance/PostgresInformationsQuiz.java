package fr.iutmontreuil.quizzard.features.seance.infrastructure.persistance;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.iutmontreuil.quizzard.features.seance.application.dto.TitresQuiz;
import fr.iutmontreuil.quizzard.features.seance.application.port.InformationsQuiz;

/**
 * Lecture seule des titres du quiz et de son cours, par une requête SQL propre à la feature {@code seance} :
 * elle ne passe ni par les entités ni par l'infrastructure des features {@code quiz} et {@code cours}.
 */
@Repository
class PostgresInformationsQuiz implements InformationsQuiz {

    private final JdbcClient jdbc;

    PostgresInformationsQuiz(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public TitresQuiz titresDeLaSession(Long idSession) {
        return jdbc.sql("""
                        SELECT c.titre AS titre_cours, q.titre AS titre_quiz
                        FROM session_quiz s
                        JOIN quiz q ON q.id_quiz = s.id_quiz
                        JOIN cours c ON c.id_cours = q.id_cours
                        WHERE s.id_session = :idSession
                        """)
                .param("idSession", idSession)
                .query((rs, ligne) -> new TitresQuiz(rs.getString("titre_cours"), rs.getString("titre_quiz")))
                .optional()
                // La séance vient d'être lue et les clés étrangères garantissent son quiz et son cours.
                .orElseThrow(() -> new IllegalStateException("Titres introuvables pour la séance " + idSession + "."));
    }
}
