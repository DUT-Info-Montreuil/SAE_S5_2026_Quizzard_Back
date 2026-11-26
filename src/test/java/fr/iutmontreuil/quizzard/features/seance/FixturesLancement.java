package fr.iutmontreuil.quizzard.features.seance;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Données de test de l'UC7, écrites en SQL : l'application ne sait pas encore créer de cours ni de quiz. Chaque
 * enseignant est unique, pour que les tests ne se gênent pas ; {@link #nettoyer()} supprime tout ce qui dépend des
 * enseignants créés, séances comprises.
 */
public class FixturesLancement {

    public static final String TITRE_COURS = "Réseaux";
    public static final String TITRE_QUIZ = "Quiz du chapitre 3";

    /** Un enseignant et son identifiant de connexion. */
    public record Enseignant(Long id, String identifiant) {
    }

    /** Un cours avec sa première version et un quiz encore vide. */
    public record QuizDeTest(Enseignant enseignant, Long idCours, Long idVersion, Long idQuiz) {
    }

    private final JdbcClient jdbc;
    private final List<Long> enseignants = new ArrayList<>();

    public FixturesLancement(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Enseignant creerEnseignant() {
        String identifiant = "ens-" + UUID.randomUUID();
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

    public QuizDeTest creerQuiz(boolean estNote) {
        return creerQuiz(creerEnseignant(), estNote);
    }

    public QuizDeTest creerQuiz(Enseignant enseignant, boolean estNote) {
        Long idCours = jdbc.sql("""
                        INSERT INTO cours (titre, date_creation, date_modification, archive, id_enseignant)
                        VALUES (:titre, :maintenant, :maintenant, FALSE, :idEnseignant)
                        RETURNING id_cours
                        """)
                .param("titre", TITRE_COURS)
                .param("maintenant", LocalDateTime.now())
                .param("idEnseignant", enseignant.id())
                .query(Long.class).single();
        Long idVersion = creerVersion(idCours, 1);
        Long idQuiz = jdbc.sql("""
                        INSERT INTO quiz (titre, est_note, duree_par_defaut_sec, passage_automatique, affichage_cours,
                                          archive, id_cours)
                        VALUES (:titre, :estNote, 20, FALSE, 'PROJETE_SEUL', FALSE, :idCours)
                        RETURNING id_quiz
                        """)
                .param("titre", TITRE_QUIZ)
                .param("estNote", estNote)
                .param("idCours", idCours)
                .query(Long.class).single();
        return new QuizDeTest(enseignant, idCours, idVersion, idQuiz);
    }

    public Long creerVersion(Long idCours, int numero) {
        return jdbc.sql("""
                        INSERT INTO version_cours (numero, nom_fichier, chemin_stockage, taille_octets, nb_pages,
                                                   empreinte, date_import, id_cours)
                        VALUES (:numero, 'cours.pdf', :chemin, 10, 12, :empreinte, :maintenant, :idCours)
                        RETURNING id_version
                        """)
                .param("numero", numero)
                .param("chemin", "test/" + UUID.randomUUID() + ".pdf")
                .param("empreinte", "a".repeat(64))
                .param("maintenant", LocalDateTime.now())
                .param("idCours", idCours)
                .query(Long.class).single();
    }

    /** QCM à une bonne réponse (« LDAP », devant « SMTP »), placé après la page {@code numeroPage}. */
    public Long ajouterQcm(QuizDeTest quiz, int numeroPage, String enonce, Integer dureeSec) {
        Long idQuestion = ajouterQuestion(quiz, "QCM", enonce, dureeSec, numeroPage);
        jdbc.sql("""
                        INSERT INTO proposition (libelle, est_correcte, rang, id_question)
                        VALUES ('LDAP', TRUE, 0, :id), ('SMTP', FALSE, 1, :id)
                        """)
                .param("id", idQuestion).update();
        return idQuestion;
    }

    public Long ajouterRelier(QuizDeTest quiz, int numeroPage) {
        Long idQuestion = ajouterQuestion(quiz, "POINTS_A_RELIER", "Relie chaque port", 30, numeroPage);
        jdbc.sql("""
                        INSERT INTO paire (element_gauche, element_droit, id_question)
                        VALUES ('80', 'HTTP', :id), ('53', 'DNS', :id), ('22', 'SSH', :id)
                        """)
                .param("id", idQuestion).update();
        return idQuestion;
    }

    public Long ajouterLibre(QuizDeTest quiz, int numeroPage) {
        Long idQuestion = ajouterQuestion(quiz, "REPONSE_LIBRE", "Quel protocole chiffre le web ?", null,
                numeroPage);
        jdbc.sql("""
                        INSERT INTO motif (motif, mode_motif, id_question)
                        VALUES ('https', 'SIMPLE', :id), ('tls|ssl', 'REGEX', :id)
                        """)
                .param("id", idQuestion).update();
        return idQuestion;
    }

    public void ajouterEcran(Long idQuestion, String type, int rang, int delaiSec) {
        jdbc.sql("""
                        INSERT INTO ecran_intermediaire (type_ecran, rang, delai_apres_cloture_sec, id_question)
                        VALUES (:type, :rang, :delai, :id)
                        """)
                .param("type", type).param("rang", rang).param("delai", delaiSec).param("id", idQuestion)
                .update();
    }

    public void activerPassageAutomatique(QuizDeTest quiz, int delaiSec) {
        jdbc.sql("UPDATE quiz SET passage_automatique = TRUE, delai_passage_auto_sec = :delai WHERE id_quiz = :id")
                .param("delai", delaiSec).param("id", quiz.idQuiz()).update();
    }

    public Long ajouterParticipant(Long idSession, String pseudonyme, int scoreTotal) {
        return jdbc.sql("""
                        INSERT INTO participant (pseudonyme, identite_verifiee, score_total, date_entree,
                                                 jeton_reconnexion, derniere_activite, id_session)
                        VALUES (:pseudonyme, FALSE, :score, :maintenant, :empreinte, :maintenant, :idSession)
                        RETURNING id_participant
                        """)
                .param("pseudonyme", pseudonyme)
                .param("score", scoreTotal)
                .param("maintenant", LocalDateTime.now())
                .param("empreinte", (UUID.randomUUID().toString() + UUID.randomUUID()).replace("-", "")
                        .substring(0, 64))
                .param("idSession", idSession)
                .query(Long.class).single();
    }

    /** Réponse à un QCM qui coche la proposition de rang donné. */
    public void ajouterReponse(Long idParticipant, Long idQuestion, int rangCoche, int points) {
        Long idReponse = jdbc.sql("""
                        INSERT INTO reponse (horodatage_serveur, temps_reponse_ms, points_calcules, points_finaux,
                                             statut_correction, id_participant, id_question)
                        VALUES (:maintenant, 1500, :points, :points, 'SANS_OBJET', :participant, :question)
                        RETURNING id_reponse
                        """)
                .param("maintenant", LocalDateTime.now())
                .param("points", points)
                .param("participant", idParticipant)
                .param("question", idQuestion)
                .query(Long.class).single();
        jdbc.sql("""
                        INSERT INTO reponse_choix (id_reponse, id_proposition)
                        SELECT :reponse, id_proposition FROM proposition WHERE id_question = :question AND rang = :rang
                        """)
                .param("reponse", idReponse).param("question", idQuestion).param("rang", rangCoche)
                .update();
    }

    /** Identifiant de la proposition de ce rang. */
    public Long proposition(Long idQuestion, int rang) {
        return jdbc.sql("SELECT id_proposition FROM proposition WHERE id_question = :id AND rang = :rang")
                .param("id", idQuestion).param("rang", rang)
                .query(Long.class).single();
    }

    public void nettoyer() {
        String cours = "SELECT id_cours FROM cours WHERE id_enseignant = :id";
        String quiz = "SELECT id_quiz FROM quiz WHERE id_cours IN (" + cours + ")";
        String questions = "SELECT id_question FROM question WHERE id_quiz IN (" + quiz + ")";
        for (Long id : enseignants) {
            // La suppression d'une séance emporte ses participants et leurs réponses (ON DELETE CASCADE).
            for (String sql : List.of(
                    "DELETE FROM session_quiz WHERE id_quiz IN (" + quiz + ")",
                    "DELETE FROM page_inseree WHERE id_question IN (" + questions + ")",
                    "DELETE FROM ecran_intermediaire WHERE id_question IN (" + questions + ")",
                    "DELETE FROM motif WHERE id_question IN (" + questions + ")",
                    "DELETE FROM paire WHERE id_question IN (" + questions + ")",
                    "DELETE FROM proposition WHERE id_question IN (" + questions + ")",
                    "DELETE FROM question WHERE id_quiz IN (" + quiz + ")",
                    "DELETE FROM quiz WHERE id_cours IN (" + cours + ")",
                    "DELETE FROM version_cours WHERE id_cours IN (" + cours + ")",
                    "DELETE FROM cours WHERE id_enseignant = :id",
                    "DELETE FROM enseignant WHERE id_enseignant = :id")) {
                jdbc.sql(sql).param("id", id).update();
            }
        }
        enseignants.clear();
    }

    private Long ajouterQuestion(QuizDeTest quiz, String type, String enonce, Integer dureeSec, int numeroPage) {
        Long idQuestion = jdbc.sql("""
                        INSERT INTO question (type, enonce, duree_sec, points_max, decroissance_temps,
                                              points_partiels, id_quiz)
                        VALUES (:type, :enonce, :duree, 1000, TRUE, FALSE, :idQuiz)
                        RETURNING id_question
                        """)
                .param("type", type).param("enonce", enonce).param("duree", dureeSec).param("idQuiz", quiz.idQuiz())
                .query(Long.class).single();
        jdbc.sql("""
                        INSERT INTO page_inseree (numero_page, ordre, valide, id_version, id_question)
                        VALUES (:page, 0, TRUE, :idVersion, :idQuestion)
                        """)
                .param("page", numeroPage).param("idVersion", quiz.idVersion()).param("idQuestion", idQuestion)
                .update();
        return idQuestion;
    }
}
