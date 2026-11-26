package fr.iutmontreuil.quizzard.features.cours.application;

import java.nio.file.Path;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.cours.application.port.CoursRepository;
import fr.iutmontreuil.quizzard.features.cours.application.port.LectureFichiersCours;
import fr.iutmontreuil.quizzard.features.cours.domain.CoursIntrouvableException;
import fr.iutmontreuil.quizzard.features.cours.domain.FichierIntrouvableException;
import fr.iutmontreuil.quizzard.features.cours.domain.FichierVersion;
import fr.iutmontreuil.quizzard.features.cours.domain.VersionIntrouvableException;
import fr.iutmontreuil.quizzard.shared.application.port.EnseignantConnecte;

/**
 * PDF d'une version, pour l'éditeur E3 (aperçu, miniatures et repère de texte de l'UC3, rendus par PDF.js).
 * Enseignant propriétaire seulement ; l'accès des étudiants viendra avec l'affichage du cours en séance.
 */
@Service
public class LireFichierVersionUseCase {

    private final EnseignantConnecte enseignant;
    private final CoursRepository cours;
    private final LectureFichiersCours fichiers;

    public LireFichierVersionUseCase(EnseignantConnecte enseignant, CoursRepository cours,
                                     LectureFichiersCours fichiers) {
        this.enseignant = enseignant;
        this.cours = cours;
        this.fichiers = fichiers;
    }

    /**
     * @return le fichier PDF à servir
     * @throws CoursIntrouvableException   si le cours n'existe pas ou appartient à un autre enseignant
     * @throws VersionIntrouvableException si la version n'est pas de ce cours
     * @throws FichierIntrouvableException si le fichier a été supprimé (UC20) ou n'est plus sur le disque
     */
    @Transactional(readOnly = true)
    public Path executer(Long idCours, Long idVersion) {
        cours.trouver(idCours, enseignant.idEnseignant()).orElseThrow(CoursIntrouvableException::new);
        FichierVersion fichier = cours.fichierDe(idVersion, idCours).orElseThrow(VersionIntrouvableException::new);
        if (fichier.estSupprime()) {
            throw new FichierIntrouvableException();
        }
        return fichiers.fichierLisible(fichier.cheminStockage()).orElseThrow(FichierIntrouvableException::new);
    }
}
