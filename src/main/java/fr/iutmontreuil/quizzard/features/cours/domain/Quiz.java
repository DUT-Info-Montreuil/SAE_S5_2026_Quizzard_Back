package fr.iutmontreuil.quizzard.features.cours.domain;

import java.util.ArrayList;
import java.util.List;

/** Quiz d'un cours (lié au cours, pas à une version). */
public final class Quiz {

    /** Durée par défaut d'une question d'un nouveau quiz (valeur proposée par le dossier, dans les bornes de E3). */
    public static final int DUREE_PAR_DEFAUT_SEC = 20;
    public static final String AFFICHAGE_PAR_DEFAUT = "PROJETE_SEUL";

    private Long idQuiz;
    private final Long idCours;
    private final String titre;
    private final boolean estNote;
    private final int dureeParDefautSec;
    private final boolean passageAutomatique;
    private final String affichageCours;
    private final boolean archive;
    private final List<Question> nouvellesQuestions = new ArrayList<>();

    public Quiz(Long idQuiz, Long idCours, String titre, boolean estNote, int dureeParDefautSec,
                boolean passageAutomatique, String affichageCours, boolean archive) {
        this.idQuiz = idQuiz;
        this.idCours = idCours;
        this.titre = titre;
        this.estNote = estNote;
        this.dureeParDefautSec = dureeParDefautSec;
        this.passageAutomatique = passageAutomatique;
        this.affichageCours = affichageCours;
        this.archive = archive;
    }

    /**
     * Nouveau quiz avec les réglages par défaut du dossier : titre « Quiz n », non noté, 20 s par question, passage
     * manuel à la question suivante, cours projeté seul. Sans question, non encore enregistré.
     *
     * @param numero n = nombre de quiz du cours + 1
     * @throws IllegalArgumentException si numero &lt; 1 (erreur de programmation)
     */
    public static Quiz nouveau(Long idCours, int numero) {
        if (numero < 1) {
            throw new IllegalArgumentException("numero doit être au moins 1 : " + numero);
        }
        return new Quiz(null, idCours, "Quiz " + numero, false, DUREE_PAR_DEFAUT_SEC, false, AFFICHAGE_PAR_DEFAUT,
                false);
    }

    /**
     * Ajoute en fin de quiz une question vierge, sans identifiant jusqu'à l'enregistrement du quiz.
     *
     * @throws IllegalStateException si le quiz est archivé (erreur de programmation)
     */
    public Question ajouterQuestionVierge() {
        if (archive) {
            throw new IllegalStateException("quiz archivé : " + idQuiz);
        }
        Question question = Question.vierge();
        nouvellesQuestions.add(question);
        return question;
    }

    public boolean estNouveau() {
        return idQuiz == null;
    }

    public void identifier(Long id) {
        this.idQuiz = id;
    }

    /** Questions ajoutées depuis le chargement, à enregistrer. */
    public List<Question> nouvellesQuestions() {
        return List.copyOf(nouvellesQuestions);
    }

    public Long idQuiz() {
        return idQuiz;
    }

    public Long idCours() {
        return idCours;
    }

    public String titre() {
        return titre;
    }

    public boolean estNote() {
        return estNote;
    }

    public int dureeParDefautSec() {
        return dureeParDefautSec;
    }

    public boolean passageAutomatique() {
        return passageAutomatique;
    }

    public String affichageCours() {
        return affichageCours;
    }

    public boolean archive() {
        return archive;
    }
}
