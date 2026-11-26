package fr.iutmontreuil.quizzard.features.seance.domain;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/** Réponse à une question « points à relier » : les liens tracés. */
public final class ReponseRelier extends Reponse {

    private final List<Lien> liens;

    ReponseRelier(Long idParticipant, Long idQuestion, LocalDateTime horodatage, long tempsMs, List<Lien> liens) {
        super(idParticipant, idQuestion, horodatage, tempsMs);
        this.liens = List.copyOf(liens);
    }

    public List<Lien> liens() {
        return liens;
    }

    /** Éléments de cette question ; chaque élément de gauche et de droite relié au plus une fois. */
    @Override
    public void valider(Question question) {
        if (!(question instanceof QuestionPointsARelier relier)) {
            throw new ReponseInvalideException();
        }
        List<Long> gauches = liens.stream().map(Lien::gauche).toList();
        List<Long> droites = liens.stream().map(Lien::droite).toList();
        boolean elementsConnus = liens.stream().allMatch(l -> l.gauche() != null && l.droite() != null
                && relier.contient(l.gauche()) && relier.contient(l.droite()));
        if (!elementsConnus
                || new HashSet<>(gauches).size() != gauches.size()
                || new HashSet<>(droites).size() != droites.size()) {
            throw new ReponseInvalideException();
        }
    }
}
