package fr.iutmontreuil.quizzard.features.seance.application;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.seance.application.port.MinuterieSeance;
import fr.iutmontreuil.quizzard.features.seance.application.port.SeanceAnimeeRepository;
import fr.iutmontreuil.quizzard.features.seance.application.port.SeanceAnimeeRepository.QuestionOuverte;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/**
 * Au démarrage du serveur, reprogramme la clôture des questions restées ouvertes : la minuterie est en mémoire. Une
 * échéance dépassée pendant l'arrêt est traitée aussitôt. Les écrans et le passage automatique en attente sont
 * perdus : l'enseignant passe à la question suivante lui-même.
 */
@Service
public class ReprendreEcheancesUseCase {

    private final SeanceAnimeeRepository seances;
    private final MinuterieSeance minuterie;
    private final JournalEvenements journal;

    public ReprendreEcheancesUseCase(SeanceAnimeeRepository seances, MinuterieSeance minuterie,
                                     JournalEvenements journal) {
        this.seances = seances;
        this.minuterie = minuterie;
        this.journal = journal;
    }

    @Transactional(readOnly = true)
    public void executer() {
        List<QuestionOuverte> ouvertes = seances.listerQuestionsOuvertes();
        ouvertes.forEach(q -> minuterie.programmerCloture(q.idSession(), q.position(), q.echeance()));
        if (!ouvertes.isEmpty()) {
            journal.evenement("echeances_reprises", Map.of("nombre", ouvertes.size()));
        }
    }
}
