package fr.iutmontreuil.quizzard.features.seance.domain;

import java.util.List;

/**
 * Question à réponse libre : la réponse est acceptée si un motif correspond au texte normalisé. Tout ou rien, sans
 * points partiels ; les points sont provisoires jusqu'à la correction par l'enseignant (UC16).
 */
public final class QuestionReponseLibre extends Question {

    private final List<Motif> motifs;
    private final VerificateurMotif verificateur;

    public QuestionReponseLibre(Long idQuestion, String enonce, int dureeSec, Bareme bareme, List<Motif> motifs,
                                VerificateurMotif verificateur) {
        super(idQuestion, enonce, dureeSec, bareme);
        this.motifs = List.copyOf(motifs);
        this.verificateur = verificateur;
    }

    @Override
    public TypeQuestion type() {
        return TypeQuestion.REPONSE_LIBRE;
    }

    public List<Motif> motifs() {
        return motifs;
    }

    /** Vrai si au moins un motif accepte le texte, une fois normalisé. */
    public boolean accepte(String texte) {
        String normalise = TexteReponse.normaliser(texte);
        return motifs.stream().anyMatch(m -> verificateur.accepte(m, normalise));
    }

    @Override
    protected double fractionJuste(Reponse reponse) {
        return accepte(attendue(reponse, ReponseLibre.class).texteLibre()) ? 1 : 0;
    }

    /** ACCEPTEE_PAR_MOTIF si un motif correspond, sinon A_EXAMINER (décision de l'enseignant après la séance). */
    @Override
    public StatutCorrection statutCorrection(Reponse reponse) {
        return accepte(attendue(reponse, ReponseLibre.class).texteLibre())
                ? StatutCorrection.ACCEPTEE_PAR_MOTIF
                : StatutCorrection.A_EXAMINER;
    }
}
