package fr.iutmontreuil.quizzard.features.cours.domain;

/**
 * Page de quiz insérée dans une version du cours : elle porte une seule question et se place après la page
 * {@code numeroPage} du PDF, au rang {@code ordre} parmi les pages insérées au même endroit.
 */
public final class PageQuiz {

    /** Longueur maximale du repère de texte (dossier : 300 caractères). */
    public static final int LONGUEUR_MAX_REPERE = 300;

    private Long idPage;
    private final Long idVersion;
    private final int numeroPage;
    private final int ordre;
    private final String repereTexte;
    private final Long idQuestion;

    private PageQuiz(Long idVersion, int numeroPage, int ordre, String repereTexte, Long idQuestion) {
        this.idVersion = idVersion;
        this.numeroPage = numeroPage;
        this.ordre = ordre;
        this.repereTexte = repereTexte;
        this.idQuestion = idQuestion;
    }

    /**
     * Nouvelle page, position confirmée ({@code valide}). Le repère de texte est nettoyé : espaces normalisés, vide
     * remplacé par {@code null}, 300 caractères au plus.
     *
     * @throws IllegalArgumentException si idVersion ou idQuestion est nul (erreur de programmation)
     */
    public static PageQuiz creer(Long idVersion, int numeroPage, int ordre, String repereTexte, Long idQuestion) {
        if (idVersion == null || idQuestion == null) {
            throw new IllegalArgumentException("idVersion et idQuestion sont obligatoires");
        }
        return new PageQuiz(idVersion, numeroPage, ordre, nettoyer(repereTexte), idQuestion);
    }

    static String nettoyer(String repere) {
        if (repere == null) {
            return null;
        }
        String propre = repere.strip().replaceAll("\\s+", " ");
        if (propre.isEmpty()) {
            return null;
        }
        return propre.length() > LONGUEUR_MAX_REPERE ? propre.substring(0, LONGUEUR_MAX_REPERE) : propre;
    }

    public boolean valide() {
        return true;
    }

    public void identifier(Long id) {
        this.idPage = id;
    }

    public Long idPage() {
        return idPage;
    }

    public Long idVersion() {
        return idVersion;
    }

    public int numeroPage() {
        return numeroPage;
    }

    public int ordre() {
        return ordre;
    }

    public String repereTexte() {
        return repereTexte;
    }

    public Long idQuestion() {
        return idQuestion;
    }
}
