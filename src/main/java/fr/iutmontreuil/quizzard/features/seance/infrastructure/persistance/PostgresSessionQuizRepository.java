package fr.iutmontreuil.quizzard.features.seance.infrastructure.persistance;

import java.sql.Timestamp;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.iutmontreuil.quizzard.features.seance.application.port.SessionQuizRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.EtatSession;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionQuiz;

/**
 * Séances lues avec leur question ouverte. Les questions d'un quiz n'ont pas d'ordre propre : il vient des pages de
 * quiz insérées dans la version du cours projetée (dossier de conception, PAGE_INSEREE). La question ouverte est la
 * page de quiz de rang {@code position_courante} (1 pour la première), pages triées par page du PDF puis par ordre.
 */
@Repository
class PostgresSessionQuizRepository implements SessionQuizRepository {

    private final JdbcClient jdbc;

    PostgresSessionQuizRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<SessionQuiz> trouverParCodeAcces(String codeAcces) {
        return jdbc.sql("""
                        SELECT s.id_session, s.code_acces, s.etat, s.est_notee, s.position_courante,
                               s.echeance_question,
                               (SELECT pages.id_question
                                  FROM (SELECT p.id_question,
                                               row_number() OVER (ORDER BY p.numero_page, p.ordre) AS rang
                                          FROM page_inseree p
                                          JOIN question q ON q.id_question = p.id_question
                                         WHERE p.id_version = s.id_version AND q.id_quiz = s.id_quiz) pages
                                 WHERE pages.rang = s.position_courante) AS id_question_ouverte
                          FROM session_quiz s
                         WHERE s.code_acces = :codeAcces
                        """)
                .param("codeAcces", codeAcces)
                .query((ligne, i) -> {
                    Timestamp echeance = ligne.getTimestamp("echeance_question");
                    return new SessionQuiz(
                            ligne.getLong("id_session"),
                            ligne.getString("code_acces"),
                            EtatSession.valueOf(ligne.getString("etat")),
                            ligne.getBoolean("est_notee"),
                            ligne.getInt("position_courante"),
                            ligne.getObject("id_question_ouverte", Long.class),
                            echeance == null ? null : echeance.toLocalDateTime());
                })
                .optional();
    }
}
