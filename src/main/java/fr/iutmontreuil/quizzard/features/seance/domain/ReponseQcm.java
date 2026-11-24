package fr.iutmontreuil.quizzard.features.seance.domain;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/** Réponse à un QCM : les propositions cochées. */
public final class ReponseQcm extends Reponse {

    private final List<Long> choix;

    ReponseQcm(Long idParticipant, Long idQuestion, LocalDateTime horodatage, long tempsMs, List<Long> choix) {
        super(idParticipant, idQuestion, horodatage, tempsMs);
        this.choix = List.copyOf(choix);
    }

    public List<Long> choix() {
        return choix;
    }

    /** Propositions de cette question, sans doublon ; une seule si la question n'a qu'une bonne réponse. */
    @Override
    public void valider(Question question) {
        if (!(question instanceof QuestionQcm qcm)
                || new HashSet<>(choix).size() != choix.size()
                || !choix.stream().allMatch(qcm::contient)
                || (!qcm.choixMultiple() && choix.size() > 1)) {
            throw new ReponseInvalideException();
        }
    }
}
