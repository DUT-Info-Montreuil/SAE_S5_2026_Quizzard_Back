package fr.iutmontreuil.quizzard.features.seance.domain;

import java.util.List;

/** Question « points à relier » : au moins 2 paires ; l'étudiant relie chaque élément de gauche à un de droite. */
public final class QuestionPointsARelier extends Question {

    private final List<Paire> paires;

    public QuestionPointsARelier(Long idQuestion, String enonce, int dureeSec, Bareme bareme, List<Paire> paires) {
        super(idQuestion, enonce, dureeSec, bareme);
        this.paires = List.copyOf(paires);
    }

    @Override
    public TypeQuestion type() {
        return TypeQuestion.POINTS_A_RELIER;
    }

    public List<Paire> paires() {
        return paires;
    }

    public boolean contient(Long idPaire) {
        return paires.stream().anyMatch(p -> p.idPaire().equals(idPaire));
    }

    /** Sans points partiels : 1 si toutes les paires sont justes, sinon 0. Avec : paires justes / nombre de paires. */
    @Override
    protected double fractionJuste(Reponse reponse) {
        if (paires.isEmpty()) {
            return 0;
        }
        long justes = attendue(reponse, ReponseRelier.class).liens().stream()
                .filter(Lien::estJuste).filter(l -> contient(l.gauche())).count();
        if (!bareme().pointsPartiels()) {
            return justes == paires.size() ? 1 : 0;
        }
        return (double) justes / paires.size();
    }
}
