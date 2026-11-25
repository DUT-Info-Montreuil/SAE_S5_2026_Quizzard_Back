package fr.iutmontreuil.quizzard.features.seance.infrastructure.persistance;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.iutmontreuil.quizzard.features.seance.application.dto.RepartitionReponses;
import fr.iutmontreuil.quizzard.features.seance.application.port.SuiviSeanceRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.ScoreParticipant;

/** Lecture des participants et de leurs réponses, sans aucune donnée personnelle. */
@Repository
class PostgresSuiviSeanceRepository implements SuiviSeanceRepository {

    private final JdbcClient jdbc;

    PostgresSuiviSeanceRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int compterConnectes(Long idSession) {
        return jdbc.sql("SELECT count(*) FROM participant WHERE id_session = :idSession")
                .param("idSession", idSession)
                .query(Integer.class)
                .single();
    }

    @Override
    public RepartitionReponses repartition(Long idSession, Long idQuestion) {
        int total = jdbc.sql("""
                        SELECT count(*) FROM reponse r
                          JOIN participant p ON p.id_participant = r.id_participant
                         WHERE p.id_session = :idSession AND r.id_question = :idQuestion
                        """)
                .param("idSession", idSession)
                .param("idQuestion", idQuestion)
                .query(Integer.class)
                .single();
        Map<Long, Integer> parProposition = new HashMap<>();
        jdbc.sql("""
                        SELECT c.id_proposition, count(*) AS nombre FROM reponse_choix c
                          JOIN reponse r ON r.id_reponse = c.id_reponse
                          JOIN participant p ON p.id_participant = r.id_participant
                         WHERE p.id_session = :idSession AND r.id_question = :idQuestion
                         GROUP BY c.id_proposition
                        """)
                .param("idSession", idSession)
                .param("idQuestion", idQuestion)
                .query((ResultSet l) -> {
                    parProposition.put(l.getLong("id_proposition"), l.getInt("nombre"));
                });
        return new RepartitionReponses(total, parProposition);
    }

    @Override
    public List<ScoreParticipant> scores(Long idSession, Long idQuestion) {
        // Une réponse au plus par participant et par question : les agrégats filtrés lisent cette seule ligne.
        return jdbc.sql("""
                        SELECT p.id_participant, p.score_total, p.date_entree,
                               COALESCE(SUM(r.temps_reponse_ms), 0) AS temps_total,
                               COUNT(r.id_reponse) FILTER (WHERE r.id_question = :idQuestion) AS a_repondu,
                               COALESCE(SUM(r.points_finaux) FILTER (WHERE r.id_question = :idQuestion), 0) AS points,
                               COALESCE(SUM(r.temps_reponse_ms) FILTER (WHERE r.id_question = :idQuestion), 0) AS temps
                          FROM participant p
                          LEFT JOIN reponse r ON r.id_participant = p.id_participant
                         WHERE p.id_session = :idSession
                         GROUP BY p.id_participant
                         ORDER BY p.id_participant
                        """)
                .param("idSession", idSession)
                .param("idQuestion", idQuestion)
                .query((l, i) -> new ScoreParticipant(l.getLong("id_participant"), l.getInt("score_total"),
                        l.getLong("temps_total"), l.getTimestamp("date_entree").toLocalDateTime(),
                        l.getInt("a_repondu") > 0, l.getInt("points"), l.getLong("temps")))
                .list();
    }
}
