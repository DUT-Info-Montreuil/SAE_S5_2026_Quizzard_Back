package fr.iutmontreuil.quizzard.features.cours.infrastructure.persistance;

import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.iutmontreuil.quizzard.features.cours.application.port.QuizRepository;
import fr.iutmontreuil.quizzard.features.cours.domain.Question;
import fr.iutmontreuil.quizzard.features.cours.domain.Quiz;

/** Quiz et questions vierges, en SQL. */
@Repository
class PostgresQuizRepository implements QuizRepository {

    private final JdbcClient jdbc;

    PostgresQuizRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Quiz> trouverActif(Long idQuiz, Long idCours) {
        return jdbc.sql("""
                        SELECT id_quiz, id_cours, titre, est_note, duree_par_defaut_sec, passage_automatique,
                               affichage_cours, archive
                          FROM quiz
                         WHERE id_quiz = :idQuiz AND id_cours = :idCours AND NOT archive
                        """)
                .param("idQuiz", idQuiz)
                .param("idCours", idCours)
                .query((l, i) -> new Quiz(l.getLong("id_quiz"), l.getLong("id_cours"), l.getString("titre"),
                        l.getBoolean("est_note"), l.getInt("duree_par_defaut_sec"), l.getBoolean("passage_automatique"),
                        l.getString("affichage_cours"), l.getBoolean("archive")))
                .optional();
    }

    @Override
    public int compterQuiz(Long idCours) {
        return jdbc.sql("SELECT count(*) FROM quiz WHERE id_cours = :idCours")
                .param("idCours", idCours).query(Integer.class).single();
    }

    @Override
    public boolean estEnSeance(Long idQuiz) {
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM session_quiz WHERE id_quiz = :idQuiz AND etat = 'EN_COURS')")
                .param("idQuiz", idQuiz).query(Boolean.class).single();
    }

    @Override
    public Quiz enregistrer(Quiz quiz) {
        if (quiz.estNouveau()) {
            quiz.identifier(jdbc.sql("""
                            INSERT INTO quiz (titre, est_note, duree_par_defaut_sec, passage_automatique,
                                              affichage_cours, archive, id_cours)
                            VALUES (:titre, :estNote, :duree, :passageAuto, :affichage, :archive, :idCours)
                            RETURNING id_quiz
                            """)
                    .param("titre", quiz.titre())
                    .param("estNote", quiz.estNote())
                    .param("duree", quiz.dureeParDefautSec())
                    .param("passageAuto", quiz.passageAutomatique())
                    .param("affichage", quiz.affichageCours())
                    .param("archive", quiz.archive())
                    .param("idCours", quiz.idCours())
                    .query(Long.class).single());
        }
        for (Question question : quiz.nouvellesQuestions()) {
            question.identifier(jdbc.sql("""
                            INSERT INTO question (type, enonce, duree_sec, points_max, decroissance_temps,
                                                  points_partiels, verrouillee, archivee, id_quiz)
                            VALUES (:type, :enonce, :duree, :pointsMax, :decroissance, :partiels, FALSE, FALSE,
                                    :idQuiz)
                            RETURNING id_question
                            """)
                    .param("type", question.type())
                    .param("enonce", question.enonce())
                    .param("duree", question.dureeSec())
                    .param("pointsMax", question.pointsMax())
                    .param("decroissance", question.decroissanceTemps())
                    .param("partiels", question.pointsPartiels())
                    .param("idQuiz", quiz.idQuiz())
                    .query(Long.class).single());
        }
        return quiz;
    }
}
