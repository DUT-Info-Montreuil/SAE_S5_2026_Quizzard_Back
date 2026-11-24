package fr.iutmontreuil.quizzard.features.seance.domain;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Question à choix multiples : au moins 2 propositions, au moins une correcte. */
public final class QuestionQcm extends Question {

    private final List<Proposition> propositions;

    public QuestionQcm(Long idQuestion, String enonce, int dureeSec, Bareme bareme, List<Proposition> propositions) {
        super(idQuestion, enonce, dureeSec, bareme);
        this.propositions = List.copyOf(propositions);
    }

    @Override
    public TypeQuestion type() {
        return TypeQuestion.QCM;
    }

    public List<Proposition> propositions() {
        return propositions;
    }

    /** Vrai si plusieurs propositions sont correctes : l'étudiant peut alors en cocher plusieurs. */
    public boolean choixMultiple() {
        return idCorrectes().size() > 1;
    }

    public boolean contient(Long idProposition) {
        return propositions.stream().anyMatch(p -> p.idProposition().equals(idProposition));
    }

    /**
     * Sans points partiels : 1 si les propositions cochées sont exactement les correctes, sinon 0.
     * Avec : {@code max(0, (bonnes cochées − mauvaises cochées) / nombre de bonnes)}.
     */
    @Override
    protected double fractionJuste(Reponse reponse) {
        Set<Long> cochees = Set.copyOf(attendue(reponse, ReponseQcm.class).choix());
        Set<Long> correctes = idCorrectes();
        if (correctes.isEmpty()) {
            return 0;
        }
        if (!bareme().pointsPartiels()) {
            return cochees.equals(correctes) ? 1 : 0;
        }
        long bonnes = cochees.stream().filter(correctes::contains).count();
        long mauvaises = cochees.size() - bonnes;
        return Math.max(0, (double) (bonnes - mauvaises) / correctes.size());
    }

    private Set<Long> idCorrectes() {
        return propositions.stream().filter(Proposition::estCorrecte).map(Proposition::idProposition)
                .collect(Collectors.toUnmodifiableSet());
    }
}
