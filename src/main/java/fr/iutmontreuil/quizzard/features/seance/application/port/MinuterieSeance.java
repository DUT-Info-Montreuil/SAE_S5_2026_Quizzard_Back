package fr.iutmontreuil.quizzard.features.seance.application.port;

import java.time.LocalDateTime;

import fr.iutmontreuil.quizzard.features.seance.domain.EcranIntermediaire;

/**
 * Tâches du déroulement que le serveur exécute lui-même, à une date de son horloge. Une tâche demandée dans une
 * transaction n'est programmée qu'après sa validation. Au moment de l'exécution, la séance a pu avancer : chaque
 * tâche vérifie que la position n'a pas changé avant d'agir.
 */
public interface MinuterieSeance {

    /** Clôture automatique de la question à son échéance. */
    void programmerCloture(Long idSession, int position, LocalDateTime echeance);

    /** Affichage d'un écran intermédiaire de la question close. */
    void programmerEcran(Long idSession, int position, EcranIntermediaire ecran, LocalDateTime moment);

    /** Passage automatique à la question qui suit la position donnée. */
    void programmerQuestionSuivante(Long idSession, int position, LocalDateTime moment);
}
