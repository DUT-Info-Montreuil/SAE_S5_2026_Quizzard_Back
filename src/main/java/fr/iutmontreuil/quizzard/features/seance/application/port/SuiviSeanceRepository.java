package fr.iutmontreuil.quizzard.features.seance.application.port;

import java.util.List;

import fr.iutmontreuil.quizzard.features.seance.application.dto.RepartitionReponses;
import fr.iutmontreuil.quizzard.features.seance.domain.ScoreParticipant;

/** Lecture de ce que font les participants d'une séance. Leur entrée et leurs réponses relèvent de l'UC8 et l'UC9. */
public interface SuiviSeanceRepository {

    /** Nombre de participants entrés dans la séance. */
    int compterConnectes(Long idSession);

    /** Réponses de cette séance à la question : une question peut être jouée dans plusieurs séances. */
    RepartitionReponses repartition(Long idSession, Long idQuestion);

    /** Score de chaque participant, avec ce que lui a rapporté la question. */
    List<ScoreParticipant> scores(Long idSession, Long idQuestion);
}
