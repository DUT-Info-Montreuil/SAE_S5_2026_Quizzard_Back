package fr.iutmontreuil.quizzard.features.seance.application.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import fr.iutmontreuil.quizzard.features.seance.application.dto.SeanceOuverte;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;

/**
 * Séances animées par un enseignant (UC7) ; le dossier de conception nomme ce port {@code SessionRepository}.
 */
public interface SeanceAnimeeRepository {

    /** Question ouverte d'une séance, avec son échéance : sert à reprogrammer sa clôture. */
    record QuestionOuverte(Long idSession, int position, LocalDateTime echeance) {
    }

    /**
     * Verrouille l'enseignant jusqu'à la fin de la transaction : deux lancements simultanés s'exécutent l'un après
     * l'autre, le second voit la séance créée par le premier.
     */
    void verrouillerLancements(Long idEnseignant);

    /** La séance non terminée de l'enseignant, s'il en a une. */
    Optional<SeanceOuverte> trouverOuverteParEnseignant(Long idEnseignant);

    /**
     * Enregistre une nouvelle séance en forçant l'écriture, pour qu'une erreur de base soit détectée avant la fin du
     * cas d'usage.
     *
     * @return la séance avec son identifiant
     */
    SeanceAnimee enregistrer(SeanceAnimee seance);

    Optional<SeanceAnimee> trouverParId(Long idSession);

    /**
     * Même lecture, en verrouillant la séance jusqu'à la fin de la transaction : une commande de l'enseignant et une
     * tâche planifiée ne la modifient pas en même temps.
     */
    Optional<SeanceAnimee> trouverPourModification(Long idSession);

    /** Écrit l'état, la position, l'échéance et la date de clôture d'une séance existante. */
    void mettreAJour(SeanceAnimee seance);

    List<QuestionOuverte> listerQuestionsOuvertes();
}
