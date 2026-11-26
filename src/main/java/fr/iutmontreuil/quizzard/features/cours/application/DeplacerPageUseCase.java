package fr.iutmontreuil.quizzard.features.cours.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.cours.application.AccesPageModifiable.PageModifiable;
import fr.iutmontreuil.quizzard.features.cours.application.port.CoursRepository;
import fr.iutmontreuil.quizzard.features.cours.application.port.PageInsereeRepository;
import fr.iutmontreuil.quizzard.features.cours.domain.EmplacementInvalideException;
import fr.iutmontreuil.quizzard.features.cours.domain.PageInseree;
import fr.iutmontreuil.quizzard.features.cours.domain.PagePlacee;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/**
 * Déplacer une page insérée (éditeur E3). Mêmes règles d'emplacement que l'insertion : {@code numeroPage} de 0 à
 * nbPages, {@code ordre} parmi les autres pages déjà à cet endroit (de 0 à leur nombre). Les rangs de l'ancien et
 * du nouvel endroit restent consécutifs à partir de 0.
 */
@Service
public class DeplacerPageUseCase {

    private final AccesPageModifiable acces;
    private final PageInsereeRepository pages;
    private final CoursRepository cours;
    private final JournalEvenements journal;
    private final Clock horloge;

    public DeplacerPageUseCase(AccesPageModifiable acces, PageInsereeRepository pages, CoursRepository cours,
                               JournalEvenements journal, Clock horloge) {
        this.acces = acces;
        this.pages = pages;
        this.cours = cours;
        this.journal = journal;
        this.horloge = horloge;
    }

    /**
     * @return les pages insérées de la version, à jour et dans l'ordre
     * @throws EmplacementInvalideException si le nouvel emplacement est hors du cours
     */
    @Transactional
    public List<PageInseree> executer(Long idPage, int numeroPage, int ordre) {
        PageModifiable modifiable = acces.verifier(idPage);
        PagePlacee page = modifiable.page();
        Long idVersion = page.idVersion();
        modifiable.version().verifierEmplacement(numeroPage, ordre,
                pages.compterAuMemeEndroitSauf(idVersion, numeroPage, page.idPage()));

        // Sortir la page de son ancien endroit, faire de la place au nouveau, puis l'y poser.
        pages.recaler(idVersion, page.numeroPage(), page.idPage());
        pages.decalerOrdresSauf(idVersion, numeroPage, ordre, page.idPage());
        pages.placer(page.idPage(), numeroPage, ordre);

        cours.marquerModifie(page.idCours(), LocalDateTime.now(horloge));
        journal.evenement("page_quiz_deplacee", Map.of("idPage", page.idPage(), "idVersion", idVersion));
        return pages.listerParVersion(idVersion);
    }
}
