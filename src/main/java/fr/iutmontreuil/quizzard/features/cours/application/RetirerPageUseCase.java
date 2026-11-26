package fr.iutmontreuil.quizzard.features.cours.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.cours.application.AccesPageModifiable.PageModifiable;
import fr.iutmontreuil.quizzard.features.cours.application.port.CoursRepository;
import fr.iutmontreuil.quizzard.features.cours.application.port.PageInsereeRepository;
import fr.iutmontreuil.quizzard.features.cours.domain.PagePlacee;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/**
 * Retirer une page de quiz du cours (éditeur E3 ; le front demande une confirmation avant, règle de l'UC3). La
 * question et le quiz sont conservés, même si le quiz n'a plus de page. Les pages restées au même endroit sont
 * renumérotées à partir de 0. Action sensible (suppression), journalisée.
 */
@Service
public class RetirerPageUseCase {

    private final AccesPageModifiable acces;
    private final PageInsereeRepository pages;
    private final CoursRepository cours;
    private final JournalEvenements journal;
    private final Clock horloge;

    public RetirerPageUseCase(AccesPageModifiable acces, PageInsereeRepository pages, CoursRepository cours,
                              JournalEvenements journal, Clock horloge) {
        this.acces = acces;
        this.pages = pages;
        this.cours = cours;
        this.journal = journal;
        this.horloge = horloge;
    }

    @Transactional
    public void executer(Long idPage) {
        PageModifiable modifiable = acces.verifier(idPage);
        PagePlacee page = modifiable.page();
        pages.supprimer(page.idPage());
        pages.recaler(page.idVersion(), page.numeroPage(), null);
        cours.marquerModifie(page.idCours(), LocalDateTime.now(horloge));
        journal.actionSensible("page_quiz_retiree", Map.of("cible", "page " + page.idPage(),
                "idCours", page.idCours(), "idVersion", page.idVersion()));
    }
}
