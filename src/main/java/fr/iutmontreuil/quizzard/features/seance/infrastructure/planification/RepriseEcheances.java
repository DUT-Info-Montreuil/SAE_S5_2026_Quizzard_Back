package fr.iutmontreuil.quizzard.features.seance.infrastructure.planification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.features.seance.application.ReprendreEcheancesUseCase;

/** Au démarrage de l'application, reprend la clôture des questions restées ouvertes. */
@Component
class RepriseEcheances {

    private static final Logger journal = LoggerFactory.getLogger("metier");

    private final ReprendreEcheancesUseCase reprendreEcheances;

    RepriseEcheances(ReprendreEcheancesUseCase reprendreEcheances) {
        this.reprendreEcheances = reprendreEcheances;
    }

    @EventListener(ApplicationReadyEvent.class)
    void auDemarrage() {
        try {
            reprendreEcheances.executer();
        } catch (RuntimeException e) {
            // L'application démarre quand même : les réponses hors délai restent refusées.
            journal.atWarn().addKeyValue("exception", e.getClass().getSimpleName()).log("reprise_echeances_echouee");
        }
    }
}
