package fr.iutmontreuil.quizzard.features.cours.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.cours.application.port.CoursRepository;
import fr.iutmontreuil.quizzard.features.cours.application.port.PageInsereeRepository;
import fr.iutmontreuil.quizzard.features.cours.domain.CoursIntrouvableException;
import fr.iutmontreuil.quizzard.features.cours.domain.PageInseree;
import fr.iutmontreuil.quizzard.features.cours.domain.VersionIntrouvableException;
import fr.iutmontreuil.quizzard.shared.application.port.EnseignantConnecte;

/**
 * Pages insérées d'une version, pour l'éditeur E3 (résultat attendu de l'UC3 : « la page de quiz apparaît dans la
 * liste des pages »). Toute version du cours se lit, pas seulement la dernière.
 */
@Service
public class ListerPagesUseCase {

    private final EnseignantConnecte enseignant;
    private final CoursRepository cours;
    private final PageInsereeRepository pages;

    public ListerPagesUseCase(EnseignantConnecte enseignant, CoursRepository cours, PageInsereeRepository pages) {
        this.enseignant = enseignant;
        this.cours = cours;
        this.pages = pages;
    }

    /**
     * @throws CoursIntrouvableException  si le cours n'existe pas ou appartient à un autre enseignant
     * @throws VersionIntrouvableException si la version n'est pas de ce cours
     */
    @Transactional(readOnly = true)
    public List<PageInseree> executer(Long idCours, Long idVersion) {
        cours.trouver(idCours, enseignant.idEnseignant()).orElseThrow(CoursIntrouvableException::new);
        if (!cours.versionAppartient(idVersion, idCours)) {
            throw new VersionIntrouvableException();
        }
        return pages.listerParVersion(idVersion);
    }
}
