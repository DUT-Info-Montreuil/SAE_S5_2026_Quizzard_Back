package fr.iutmontreuil.quizzard.features.seance.domain;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Agrégat de la séance vu par l'enseignant qui l'anime (UC7) ; le dossier de conception la nomme {@code Session}.
 * <p>
 * Elle est lancée à l'état {@code PREPAREE}, passe {@code EN_COURS} à la première question, puis {@code CLOTUREE}.
 * Pendant {@code EN_COURS}, la question de la position courante est ouverte tant qu'une échéance est fixée ; la
 * clore efface l'échéance (DESIGN.md §4). L'horloge est toujours celle du serveur.
 */
public final class SeanceAnimee {

    private final Long idSession;
    private final Long idEnseignant;
    private final Long idQuiz;
    private final Long idVersion;
    private final JetonSession jeton;
    private final boolean modeNote;
    private final boolean identiteNonVerifieeAutorisee;
    private final LocalDateTime dateLancement;
    private EtatSession etat;
    private int positionCourante;
    private LocalDateTime echeanceQuestion;
    private LocalDateTime dateCloture;

    private SeanceAnimee(Long idSession, Long idEnseignant, Long idQuiz, Long idVersion, JetonSession jeton,
                         boolean modeNote, boolean identiteNonVerifieeAutorisee, LocalDateTime dateLancement,
                         EtatSession etat, int positionCourante, LocalDateTime echeanceQuestion,
                         LocalDateTime dateCloture) {
        this.idSession = idSession;
        this.idEnseignant = idEnseignant;
        this.idQuiz = idQuiz;
        this.idVersion = idVersion;
        this.jeton = jeton;
        this.modeNote = modeNote;
        this.identiteNonVerifieeAutorisee = identiteNonVerifieeAutorisee;
        this.dateLancement = dateLancement;
        this.etat = etat;
        this.positionCourante = positionCourante;
        this.echeanceQuestion = echeanceQuestion;
        this.dateCloture = dateCloture;
    }

    /**
     * Nouvelle séance, à l'état {@code PREPAREE} et non enregistrée. Le mode noté est repris du quiz et ne change
     * plus ensuite ; l'autorisation d'entrée en identité non vérifiée n'a de sens que pour un quiz noté.
     *
     * @throws IllegalArgumentException si un paramètre obligatoire est nul (erreur de programmation)
     */
    public static SeanceAnimee ouvrir(Quiz quiz, Long idEnseignant, JetonSession jeton,
                                      boolean identiteNonVerifieeAutorisee, LocalDateTime maintenant) {
        if (quiz == null || quiz.idVersion() == null || idEnseignant == null || jeton == null || maintenant == null) {
            throw new IllegalArgumentException("quiz, version, enseignant, jeton et date sont obligatoires");
        }
        return new SeanceAnimee(null, idEnseignant, quiz.idQuiz(), quiz.idVersion(), jeton, quiz.estNote(),
                quiz.estNote() && identiteNonVerifieeAutorisee, precision(maintenant), EtatSession.PREPAREE, 0,
                null, null);
    }

    /** Séance relue en base. */
    public static SeanceAnimee reconstituer(Long idSession, Long idEnseignant, Long idQuiz, Long idVersion,
                                            JetonSession jeton, boolean modeNote,
                                            boolean identiteNonVerifieeAutorisee, LocalDateTime dateLancement,
                                            EtatSession etat, int positionCourante, LocalDateTime echeanceQuestion,
                                            LocalDateTime dateCloture) {
        return new SeanceAnimee(idSession, idEnseignant, idQuiz, idVersion, jeton, modeNote,
                identiteNonVerifieeAutorisee, dateLancement, etat, positionCourante, echeanceQuestion, dateCloture);
    }

    /** La même séance avec l'identifiant attribué par la base. */
    public SeanceAnimee avecIdentifiant(Long identifiant) {
        return new SeanceAnimee(identifiant, idEnseignant, idQuiz, idVersion, jeton, modeNote,
                identiteNonVerifieeAutorisee, dateLancement, etat, positionCourante, echeanceQuestion, dateCloture);
    }

    /** @throws AccesSeanceRefuseException si la séance n'appartient pas à cet enseignant */
    public void verifierProprietaire(Long idEnseignantConnecte) {
        if (!Objects.equals(idEnseignant, idEnseignantConnecte)) {
            throw new AccesSeanceRefuseException();
        }
    }

    /** @throws SeanceTermineeException si la séance est close : son historique est en lecture seule */
    public void verifierNonTerminee() {
        if (estTerminee()) {
            throw new SeanceTermineeException();
        }
    }

    public boolean estTerminee() {
        return etat == EtatSession.CLOTUREE;
    }

    /** Vrai si une question attend des réponses (avant ou après son échéance). */
    public boolean aUneQuestionOuverte() {
        return etat == EtatSession.EN_COURS && echeanceQuestion != null;
    }

    /** Vrai si la question ouverte a atteint son échéance : elle doit être close. */
    public boolean echeanceAtteinte(LocalDateTime maintenant) {
        return aUneQuestionOuverte() && !maintenant.isBefore(echeanceQuestion);
    }

    /**
     * Ouvre la question de la position suivante : la séance passe {@code EN_COURS} et son échéance est fixée à
     * maintenant plus la durée de la question.
     *
     * @throws SeanceTermineeException        si la séance est close
     * @throws QuestionDejaOuverteException   si la question courante est encore ouverte
     * @throws IllegalArgumentException       si la durée n'est pas positive (erreur de programmation)
     */
    public void ouvrirQuestionSuivante(int dureeSec, LocalDateTime maintenant) {
        verifierNonTerminee();
        if (aUneQuestionOuverte()) {
            throw new QuestionDejaOuverteException();
        }
        if (dureeSec <= 0) {
            throw new IllegalArgumentException("dureeSec doit être positive : " + dureeSec);
        }
        etat = EtatSession.EN_COURS;
        positionCourante++;
        echeanceQuestion = precision(maintenant).plusSeconds(dureeSec);
    }

    /**
     * Arrête les réponses à la question ouverte.
     *
     * @throws AucuneQuestionOuverteException s'il n'y a pas de question ouverte
     */
    public void cloreQuestion() {
        if (!aUneQuestionOuverte()) {
            throw new AucuneQuestionOuverteException();
        }
        echeanceQuestion = null;
    }

    /**
     * Termine la séance : le code d'accès et les jetons des participants ne valent plus rien.
     *
     * @throws SeanceTermineeException si elle l'est déjà
     */
    public void cloturer(LocalDateTime maintenant) {
        verifierNonTerminee();
        etat = EtatSession.CLOTUREE;
        echeanceQuestion = null;
        dateCloture = precision(maintenant);
    }

    /** La milliseconde suffit au jeu, et la base ne garde pas les fractions plus fines. */
    private static LocalDateTime precision(LocalDateTime date) {
        return date.truncatedTo(ChronoUnit.MILLIS);
    }

    public Long idSession() {
        return idSession;
    }

    public Long idEnseignant() {
        return idEnseignant;
    }

    public Long idQuiz() {
        return idQuiz;
    }

    public Long idVersion() {
        return idVersion;
    }

    public JetonSession jeton() {
        return jeton;
    }

    /** Code d'accès en clair : sert aux destinations temps réel et au lien, jamais aux logs. */
    public String codeAcces() {
        return jeton.valeur();
    }

    public boolean modeNote() {
        return modeNote;
    }

    public boolean identiteNonVerifieeAutorisee() {
        return identiteNonVerifieeAutorisee;
    }

    public LocalDateTime dateLancement() {
        return dateLancement;
    }

    public EtatSession etat() {
        return etat;
    }

    public int positionCourante() {
        return positionCourante;
    }

    public LocalDateTime echeanceQuestion() {
        return echeanceQuestion;
    }

    public LocalDateTime dateCloture() {
        return dateCloture;
    }
}
