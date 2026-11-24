package fr.iutmontreuil.quizzard.features.cours.domain;

/**
 * Question créée par l'insertion d'une page de quiz : vide, à paramétrer ensuite (UC4, UC5, UC13). Tant qu'elle est
 * incomplète, le quiz ne peut pas être lancé.
 */
public final class Question {

    public static final String TYPE_PAR_DEFAUT = "QCM";
    public static final int POINTS_MAX_PAR_DEFAUT = 1000;

    private Long idQuestion;
    private final String type;
    private final String enonce;
    private final Integer dureeSec;
    private final int pointsMax;
    private final boolean decroissanceTemps;
    private final boolean pointsPartiels;

    private Question(String type, String enonce, Integer dureeSec, int pointsMax, boolean decroissanceTemps,
                     boolean pointsPartiels) {
        this.type = type;
        this.enonce = enonce;
        this.dureeSec = dureeSec;
        this.pointsMax = pointsMax;
        this.decroissanceTemps = decroissanceTemps;
        this.pointsPartiels = pointsPartiels;
    }

    /**
     * Valeurs par défaut du dossier : QCM, énoncé vide, durée du quiz ({@code dureeSec} nul), 1000 points, décroissance
     * avec le temps, sans points partiels, non verrouillée.
     */
    public static Question vierge() {
        return new Question(TYPE_PAR_DEFAUT, "", null, POINTS_MAX_PAR_DEFAUT, true, false);
    }

    public void identifier(Long id) {
        this.idQuestion = id;
    }

    public Long idQuestion() {
        return idQuestion;
    }

    public String type() {
        return type;
    }

    public String enonce() {
        return enonce;
    }

    public Integer dureeSec() {
        return dureeSec;
    }

    public int pointsMax() {
        return pointsMax;
    }

    public boolean decroissanceTemps() {
        return decroissanceTemps;
    }

    public boolean pointsPartiels() {
        return pointsPartiels;
    }
}
