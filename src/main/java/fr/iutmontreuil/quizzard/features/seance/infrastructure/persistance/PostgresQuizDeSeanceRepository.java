package fr.iutmontreuil.quizzard.features.seance.infrastructure.persistance;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.iutmontreuil.quizzard.features.seance.application.port.QuizDeSeanceRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.EcranIntermediaire;
import fr.iutmontreuil.quizzard.features.seance.domain.MotifDeSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.PaireDeSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.PropositionDeSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionDeSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.Quiz;
import fr.iutmontreuil.quizzard.features.seance.domain.TypeEcran;
import fr.iutmontreuil.quizzard.features.seance.domain.TypeQuestion;

/**
 * Lecture du quiz pour la séance, en SQL propre à la feature {@code seance} : les tables appartiennent aux features
 * {@code quiz} et {@code cours}. Les questions n'ont pas d'ordre propre : il vient des pages de quiz insérées dans la
 * version présentée, triées par page du PDF puis par ordre. C'est aussi ainsi que l'UC9 retrouve la question de la
 * position courante : les deux lectures doivent rester d'accord.
 */
@Repository
class PostgresQuizDeSeanceRepository implements QuizDeSeanceRepository {

    private static final String SELECT_QUIZ = """
            SELECT q.id_quiz, q.titre, q.est_note, q.passage_automatique, q.delai_passage_auto_sec,
                   q.duree_par_defaut_sec, c.id_enseignant, c.titre AS titre_cours,
                   (SELECT v.id_version FROM version_cours v
                     WHERE v.id_cours = c.id_cours ORDER BY v.numero DESC LIMIT 1) AS derniere_version
              FROM quiz q
              JOIN cours c ON c.id_cours = q.id_cours
             WHERE q.id_quiz = :idQuiz
            """;

    private final JdbcClient jdbc;

    PostgresQuizDeSeanceRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Ligne de la table {@code quiz}, avant la lecture de ses questions. */
    private record Entete(Long idQuiz, String titre, boolean estNote, boolean passageAutomatique, Integer delai,
                          int dureeParDefaut, Long idEnseignant, String titreCours, Long derniereVersion) {
    }

    /** Page de quiz, avant la lecture du contenu attendu de sa question. */
    private record Page(Long idQuestion, TypeQuestion type, String enonce, int dureeSec, int pageAffichee) {
    }

    @Override
    public Optional<Quiz> trouverParId(Long idQuiz) {
        return entete(SELECT_QUIZ + " AND NOT q.archive", idQuiz).map(e -> completer(e, e.derniereVersion()));
    }

    @Override
    public Optional<Quiz> trouverPourSeance(Long idQuiz, Long idVersion) {
        return entete(SELECT_QUIZ, idQuiz).map(e -> completer(e, idVersion));
    }

    @Override
    public void verrouillerQuestions(Collection<Long> idQuestions) {
        if (idQuestions.isEmpty()) {
            return;
        }
        jdbc.sql("UPDATE question SET verrouillee = TRUE WHERE id_question IN (:ids) AND NOT verrouillee")
                .param("ids", idQuestions)
                .update();
    }

    private Optional<Entete> entete(String sql, Long idQuiz) {
        return jdbc.sql(sql)
                .param("idQuiz", idQuiz)
                .query((l, i) -> new Entete(l.getLong("id_quiz"), l.getString("titre"), l.getBoolean("est_note"),
                        l.getBoolean("passage_automatique"), l.getObject("delai_passage_auto_sec", Integer.class),
                        l.getInt("duree_par_defaut_sec"), l.getLong("id_enseignant"), l.getString("titre_cours"),
                        l.getObject("derniere_version", Long.class)))
                .optional();
    }

    private Quiz completer(Entete e, Long idVersion) {
        List<QuestionDeSeance> questions = idVersion == null ? List.of() : questions(e, idVersion);
        return new Quiz(e.idQuiz(), e.idEnseignant(), e.titre(), e.titreCours(), e.estNote(), e.passageAutomatique(),
                e.delai(), idVersion, questions);
    }

    private List<QuestionDeSeance> questions(Entete e, Long idVersion) {
        // La page affichée compte aussi les pages insérées placées avant (dossier de conception, numérotation de E3).
        List<Page> pages = jdbc.sql("""
                        SELECT p.id_question, q.type, q.enonce,
                               COALESCE(q.duree_sec, :dureeParDefaut) AS duree_sec,
                               p.numero_page + 1 + (SELECT count(*) FROM page_inseree a
                                                     WHERE a.id_version = p.id_version
                                                       AND (a.numero_page, a.ordre) < (p.numero_page, p.ordre))
                                   AS page_affichee
                          FROM page_inseree p
                          JOIN question q ON q.id_question = p.id_question
                         WHERE p.id_version = :idVersion AND q.id_quiz = :idQuiz
                         ORDER BY p.numero_page, p.ordre
                        """)
                .param("dureeParDefaut", e.dureeParDefaut())
                .param("idVersion", idVersion)
                .param("idQuiz", e.idQuiz())
                .query((l, i) -> new Page(l.getLong("id_question"), TypeQuestion.valueOf(l.getString("type")),
                        l.getString("enonce"), l.getInt("duree_sec"), l.getInt("page_affichee")))
                .list();
        if (pages.isEmpty()) {
            return List.of();
        }
        List<Long> ids = pages.stream().map(Page::idQuestion).toList();

        Map<Long, List<PropositionDeSeance>> propositions = parQuestion("""
                SELECT id_question, id_proposition, libelle, est_correcte, rang FROM proposition
                 WHERE id_question IN (:ids) ORDER BY rang, id_proposition
                """, ids, l -> new PropositionDeSeance(l.getLong("id_proposition"), l.getString("libelle"),
                l.getBoolean("est_correcte"), l.getInt("rang")));
        Map<Long, List<PaireDeSeance>> paires = parQuestion("""
                SELECT id_question, id_paire, element_gauche, element_droit FROM paire
                 WHERE id_question IN (:ids) ORDER BY id_paire
                """, ids, l -> new PaireDeSeance(l.getLong("id_paire"), l.getString("element_gauche"),
                l.getString("element_droit")));
        Map<Long, List<MotifDeSeance>> motifs = parQuestion("""
                SELECT id_question, motif, mode_motif FROM motif
                 WHERE id_question IN (:ids) ORDER BY id_motif
                """, ids, l -> new MotifDeSeance(l.getString("motif"), "REGEX".equals(l.getString("mode_motif"))));
        Map<Long, List<EcranIntermediaire>> ecrans = parQuestion("""
                SELECT id_question, type_ecran, rang, delai_apres_cloture_sec FROM ecran_intermediaire
                 WHERE id_question IN (:ids) ORDER BY rang, id_ecran
                """, ids, l -> new EcranIntermediaire(TypeEcran.valueOf(l.getString("type_ecran")), l.getInt("rang"),
                l.getInt("delai_apres_cloture_sec")));

        return pages.stream()
                .map(p -> new QuestionDeSeance(p.idQuestion(), p.type(), p.enonce(), p.dureeSec(), p.pageAffichee(),
                        propositions.getOrDefault(p.idQuestion(), List.of()),
                        paires.getOrDefault(p.idQuestion(), List.of()),
                        motifs.getOrDefault(p.idQuestion(), List.of()),
                        ecrans.getOrDefault(p.idQuestion(), List.of())))
                .toList();
    }

    /** Conversion d'une ligne de contenu d'une question. */
    private interface Ligne<T> {
        T lire(ResultSet ligne) throws SQLException;
    }

    /** Une seule requête pour toutes les questions, résultat rangé par question dans l'ordre de la requête. */
    private <T> Map<Long, List<T>> parQuestion(String sql, List<Long> ids, Ligne<T> conversion) {
        Map<Long, List<T>> resultat = new HashMap<>();
        jdbc.sql(sql).param("ids", ids).query((ResultSet ligne) -> {
            resultat.computeIfAbsent(ligne.getLong("id_question"), id -> new ArrayList<>())
                    .add(conversion.lire(ligne));
        });
        return resultat;
    }
}
