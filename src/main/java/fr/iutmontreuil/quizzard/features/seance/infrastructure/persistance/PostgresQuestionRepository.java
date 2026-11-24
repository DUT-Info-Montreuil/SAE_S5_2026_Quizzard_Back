package fr.iutmontreuil.quizzard.features.seance.infrastructure.persistance;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.iutmontreuil.quizzard.features.seance.application.port.QuestionRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.Bareme;
import fr.iutmontreuil.quizzard.features.seance.domain.ModeMotif;
import fr.iutmontreuil.quizzard.features.seance.domain.Motif;
import fr.iutmontreuil.quizzard.features.seance.domain.Paire;
import fr.iutmontreuil.quizzard.features.seance.domain.Proposition;
import fr.iutmontreuil.quizzard.features.seance.domain.Question;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionPointsARelier;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionQcm;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionReponseLibre;
import fr.iutmontreuil.quizzard.features.seance.domain.TypeQuestion;
import fr.iutmontreuil.quizzard.features.seance.domain.VerificateurMotif;

/**
 * Lecture seule des questions pour la séance. Les tables appartiennent à la feature {@code quiz}, qui n'a pas
 * encore de code : la séance les lit en SQL, sans entité JPA, pour ne pas imposer de modèle à cette feature.
 */
@Repository
class PostgresQuestionRepository implements QuestionRepository {

    private final JdbcClient jdbc;
    private final VerificateurMotif verificateur;

    PostgresQuestionRepository(JdbcClient jdbc, VerificateurMotif verificateur) {
        this.jdbc = jdbc;
        this.verificateur = verificateur;
    }

    private record Entete(Long id, TypeQuestion type, String enonce, int dureeSec, Bareme bareme) {
    }

    @Override
    public Optional<Question> trouverParId(Long idQuestion) {
        return jdbc.sql("""
                        SELECT q.id_question, q.type, q.enonce,
                               COALESCE(q.duree_sec, z.duree_par_defaut_sec) AS duree_sec,
                               q.points_max, q.decroissance_temps, q.points_partiels
                          FROM question q
                          JOIN quiz z ON z.id_quiz = q.id_quiz
                         WHERE q.id_question = :idQuestion
                        """)
                .param("idQuestion", idQuestion)
                .query((ligne, i) -> new Entete(ligne.getLong("id_question"),
                        TypeQuestion.valueOf(ligne.getString("type")), ligne.getString("enonce"),
                        ligne.getInt("duree_sec"), new Bareme(ligne.getInt("points_max"),
                        ligne.getBoolean("decroissance_temps"), ligne.getBoolean("points_partiels"))))
                .optional()
                .map(this::completer);
    }

    private Question completer(Entete e) {
        return switch (e.type()) {
            case QCM -> new QuestionQcm(e.id(), e.enonce(), e.dureeSec(), e.bareme(), propositions(e.id()));
            case POINTS_A_RELIER -> new QuestionPointsARelier(e.id(), e.enonce(), e.dureeSec(), e.bareme(),
                    paires(e.id()));
            case REPONSE_LIBRE -> new QuestionReponseLibre(e.id(), e.enonce(), e.dureeSec(), e.bareme(),
                    motifs(e.id()), verificateur);
        };
    }

    private List<Proposition> propositions(Long idQuestion) {
        return jdbc.sql("""
                        SELECT id_proposition, libelle, est_correcte, rang
                          FROM proposition WHERE id_question = :idQuestion ORDER BY rang, id_proposition
                        """)
                .param("idQuestion", idQuestion)
                .query((l, i) -> new Proposition(l.getLong("id_proposition"), l.getString("libelle"),
                        l.getBoolean("est_correcte"), l.getInt("rang")))
                .list();
    }

    private List<Paire> paires(Long idQuestion) {
        return jdbc.sql("""
                        SELECT id_paire, element_gauche, element_droit
                          FROM paire WHERE id_question = :idQuestion ORDER BY id_paire
                        """)
                .param("idQuestion", idQuestion)
                .query((l, i) -> new Paire(l.getLong("id_paire"), l.getString("element_gauche"),
                        l.getString("element_droit")))
                .list();
    }

    private List<Motif> motifs(Long idQuestion) {
        return jdbc.sql("SELECT motif, mode_motif FROM motif WHERE id_question = :idQuestion ORDER BY id_motif")
                .param("idQuestion", idQuestion)
                .query((l, i) -> new Motif(l.getString("motif"), ModeMotif.valueOf(l.getString("mode_motif"))))
                .list();
    }
}
