package fr.iutmontreuil.quizzard.features.seance.infrastructure.persistance;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.iutmontreuil.quizzard.features.seance.application.dto.SeanceOuverte;
import fr.iutmontreuil.quizzard.features.seance.application.port.SeanceAnimeeRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.EtatSession;
import fr.iutmontreuil.quizzard.features.seance.domain.JetonSession;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;

/**
 * Adaptateur du port {@link SeanceAnimeeRepository}. L'écriture est forcée ({@code saveAndFlush}) pour qu'une erreur
 * de base soit détectée dans le cas d'usage, donc avant le commit et avant toute diffusion. Le propriétaire de la
 * séance est celui du cours de son quiz.
 */
@Repository
class PostgresSeanceAnimeeRepository implements SeanceAnimeeRepository {

    private final SessionJpaRepository jpa;
    private final JdbcClient jdbc;

    PostgresSeanceAnimeeRepository(SessionJpaRepository jpa, JdbcClient jdbc) {
        this.jpa = jpa;
        this.jdbc = jdbc;
    }

    @Override
    public void verrouillerLancements(Long idEnseignant) {
        // NO KEY UPDATE : bloque un autre lancement, pas les insertions qui référencent l'enseignant.
        jdbc.sql("SELECT id_enseignant FROM enseignant WHERE id_enseignant = :idEnseignant FOR NO KEY UPDATE")
                .param("idEnseignant", idEnseignant)
                .query(Long.class)
                .list();
    }

    @Override
    public Optional<SeanceOuverte> trouverOuverteParEnseignant(Long idEnseignant) {
        return jdbc.sql("""
                        SELECT s.id_session, c.titre
                          FROM session_quiz s
                          JOIN quiz q ON q.id_quiz = s.id_quiz
                          JOIN cours c ON c.id_cours = q.id_cours
                         WHERE c.id_enseignant = :idEnseignant AND s.etat <> 'CLOTUREE'
                         ORDER BY s.id_session DESC
                         LIMIT 1
                        """)
                .param("idEnseignant", idEnseignant)
                .query((l, i) -> new SeanceOuverte(l.getLong("id_session"), l.getString("titre")))
                .optional();
    }

    @Override
    public SeanceAnimee enregistrer(SeanceAnimee seance) {
        SessionEntity entite = new SessionEntity();
        entite.codeAcces = seance.codeAcces();
        entite.estNotee = seance.modeNote();
        entite.dateLancement = seance.dateLancement();
        entite.idQuiz = seance.idQuiz();
        entite.idVersion = seance.idVersion();
        entite.identiteNonVerifieeAutorisee = seance.identiteNonVerifieeAutorisee();
        reporterEtat(seance, entite);
        return seance.avecIdentifiant(jpa.saveAndFlush(entite).idSession);
    }

    @Override
    public Optional<SeanceAnimee> trouverParId(Long idSession) {
        return jpa.findById(idSession).map(this::versDomaine);
    }

    @Override
    public Optional<SeanceAnimee> trouverPourModification(Long idSession) {
        return jpa.verrouiller(idSession).map(this::versDomaine);
    }

    @Override
    public void mettreAJour(SeanceAnimee seance) {
        SessionEntity entite = jpa.findById(seance.idSession())
                .orElseThrow(() -> new IllegalStateException("Séance disparue : " + seance.idSession()));
        reporterEtat(seance, entite);
        jpa.saveAndFlush(entite);
    }

    @Override
    public List<QuestionOuverte> listerQuestionsOuvertes() {
        return jpa.findByEtatAndEcheanceQuestionIsNotNull(EtatSession.EN_COURS).stream()
                .map(s -> new QuestionOuverte(s.idSession, s.positionCourante, s.echeanceQuestion))
                .toList();
    }

    /** Ce qui change pendant le déroulement. */
    private static void reporterEtat(SeanceAnimee seance, SessionEntity entite) {
        entite.etat = seance.etat();
        entite.positionCourante = seance.positionCourante();
        entite.echeanceQuestion = seance.echeanceQuestion();
        entite.dateCloture = seance.dateCloture();
    }

    private SeanceAnimee versDomaine(SessionEntity s) {
        Long idEnseignant = jdbc.sql("""
                        SELECT c.id_enseignant FROM quiz q JOIN cours c ON c.id_cours = q.id_cours
                         WHERE q.id_quiz = :idQuiz
                        """)
                .param("idQuiz", s.idQuiz)
                .query(Long.class)
                .single();
        return SeanceAnimee.reconstituer(s.idSession, idEnseignant, s.idQuiz, s.idVersion,
                new JetonSession(s.codeAcces), s.estNotee, s.identiteNonVerifieeAutorisee, s.dateLancement, s.etat,
                s.positionCourante, s.echeanceQuestion, s.dateCloture);
    }
}
