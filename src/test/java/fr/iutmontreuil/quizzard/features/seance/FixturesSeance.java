package fr.iutmontreuil.quizzard.features.seance;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;

import fr.iutmontreuil.quizzard.features.seance.domain.EtatSession;

/**
 * Données de test des séances, écrites en SQL : la création d'une séance appartient au cas « Lancer un quiz en
 * séance ». Chaque appel crée une chaîne complète (enseignant, cours, version, quiz, séance) avec des valeurs
 * uniques, pour que les tests ne se gênent pas ; {@link #nettoyer()} supprime tout ce qui a été créé.
 */
public class FixturesSeance {

    public static final String TITRE_COURS = "Réseaux";
    public static final String TITRE_QUIZ = "Quiz du chapitre 3";

    /** Une séance créée par {@link #creerSeance}. */
    public record Seance(Long idSession, String codeAcces, Long idQuiz) {
    }

    private final JdbcClient jdbc;
    private final Deque<Runnable> suppressions = new ArrayDeque<>();

    public FixturesSeance(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Seance creerSeance(EtatSession etat, boolean estNotee) {
        String unique = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime maintenant = LocalDateTime.now();

        Long idEnseignant = inserer("""
                INSERT INTO enseignant (identifiant_ldap, nom, prenom, origine_compte, role, date_creation)
                VALUES (:ldap, 'Nom', 'Prenom', 'LDAP', 'ENSEIGNANT', :maintenant)
                RETURNING id_enseignant
                """, "ldap", "ens-" + unique, "maintenant", maintenant);
        suppressions.push(() -> supprimer("DELETE FROM enseignant WHERE id_enseignant = :id", idEnseignant));

        Long idCours = inserer("""
                INSERT INTO cours (titre, date_creation, date_modification, archive, id_enseignant)
                VALUES (:titre, :maintenant, :maintenant, FALSE, :idEnseignant)
                RETURNING id_cours
                """, "titre", TITRE_COURS, "maintenant", maintenant, "idEnseignant", idEnseignant);
        suppressions.push(() -> supprimer("DELETE FROM cours WHERE id_cours = :id", idCours));

        Long idVersion = inserer("""
                INSERT INTO version_cours (numero, nom_fichier, chemin_stockage, taille_octets, nb_pages, empreinte,
                                           date_import, id_cours)
                VALUES (1, 'cours.pdf', :chemin, 10, 5, :empreinte, :maintenant, :idCours)
                RETURNING id_version
                """, "chemin", "test/" + unique + ".pdf", "empreinte", "a".repeat(64), "maintenant", maintenant,
                "idCours", idCours);
        suppressions.push(() -> supprimer("DELETE FROM version_cours WHERE id_version = :id", idVersion));

        Long idQuiz = inserer("""
                INSERT INTO quiz (titre, est_note, duree_par_defaut_sec, passage_automatique, affichage_cours, archive,
                                  id_cours)
                VALUES (:titre, :estNote, 20, FALSE, 'PROJETE_SEUL', FALSE, :idCours)
                RETURNING id_quiz
                """, "titre", TITRE_QUIZ, "estNote", estNotee, "idCours", idCours);
        suppressions.push(() -> supprimer("DELETE FROM quiz WHERE id_quiz = :id", idQuiz));

        String codeAcces = "T" + unique.substring(0, 20);
        Long idSession = inserer("""
                INSERT INTO session_quiz (code_acces, etat, est_notee, date_lancement, position_courante, id_quiz,
                                          id_version)
                VALUES (:code, :etat, :estNotee, :maintenant, 0, :idQuiz, :idVersion)
                RETURNING id_session
                """, "code", codeAcces, "etat", etat.name(), "estNotee", estNotee, "maintenant", maintenant,
                "idQuiz", idQuiz, "idVersion", idVersion);
        // La suppression de la séance emporte ses participants (ON DELETE CASCADE).
        suppressions.push(() -> supprimer("DELETE FROM session_quiz WHERE id_session = :id", idSession));

        return new Seance(idSession, codeAcces, idQuiz);
    }

    /** Supprime, dans l'ordre inverse de la création, tout ce que ces fixtures ont créé. */
    public void nettoyer() {
        while (!suppressions.isEmpty()) {
            suppressions.pop().run();
        }
    }

    private Long inserer(String sql, Object... parametres) {
        JdbcClient.StatementSpec requete = jdbc.sql(sql);
        for (int i = 0; i < parametres.length; i += 2) {
            requete = requete.param((String) parametres[i], parametres[i + 1]);
        }
        return requete.query(Long.class).single();
    }

    private void supprimer(String sql, Long id) {
        jdbc.sql(sql).param("id", id).update();
    }
}
