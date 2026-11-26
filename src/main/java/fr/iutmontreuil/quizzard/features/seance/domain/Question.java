package fr.iutmontreuil.quizzard.features.seance.domain;

/**
 * Question jouée dans une séance (UC9). Chaque type calcule la part juste d'une réponse ; le barème la convertit en
 * points. Les réponses attendues restent dans le domaine : seules les vues pour l'étudiant sortent du serveur, sans
 * elles, avant la clôture.
 */
public abstract sealed class Question permits QuestionQcm, QuestionPointsARelier, QuestionReponseLibre {

    private final Long idQuestion;
    private final String enonce;
    private final int dureeSec;
    private final Bareme bareme;

    /**
     * @param dureeSec durée effective : celle de la question, sinon la durée par défaut du quiz
     */
    protected Question(Long idQuestion, String enonce, int dureeSec, Bareme bareme) {
        if (dureeSec <= 0) {
            throw new IllegalArgumentException("dureeSec doit être positive : " + dureeSec);
        }
        this.idQuestion = idQuestion;
        this.enonce = enonce;
        this.dureeSec = dureeSec;
        this.bareme = bareme;
    }

    public abstract TypeQuestion type();

    /** Part juste de la réponse, entre 0 et 1, selon le type et le barème (DESIGN.md §3.3). */
    protected abstract double fractionJuste(Reponse reponse);

    /** Statut de correction de la réponse : SANS_OBJET sauf pour une réponse libre. */
    public StatutCorrection statutCorrection(Reponse reponse) {
        return StatutCorrection.SANS_OBJET;
    }

    /**
     * Points calculés de la réponse : part juste puis barème. Pour une réponse libre, ces points sont provisoires
     * jusqu'à la correction par l'enseignant.
     */
    public int evaluer(Reponse reponse, long tempsMs) {
        return bareme.calculerPoints(tempsMs, dureeSec * 1000L, fractionJuste(reponse));
    }

    public Long idQuestion() {
        return idQuestion;
    }

    public String enonce() {
        return enonce;
    }

    public int dureeSec() {
        return dureeSec;
    }

    public Bareme bareme() {
        return bareme;
    }

    /** Convertit la réponse dans le type attendu par la question ; erreur de programmation sinon. */
    protected static <R extends Reponse> R attendue(Reponse reponse, Class<R> type) {
        if (!type.isInstance(reponse)) {
            throw new IllegalArgumentException("réponse de type " + reponse.getClass().getSimpleName()
                    + " pour une question qui attend " + type.getSimpleName());
        }
        return type.cast(reponse);
    }
}
