package fr.iutmontreuil.quizzard.features.cours.application.port;

import java.nio.file.Path;

import fr.iutmontreuil.quizzard.features.cours.domain.FichierNonPdfException;
import fr.iutmontreuil.quizzard.features.cours.domain.PdfIllisibleException;

/** Contrôle d'un fichier PDF. */
public interface AnalyseurPdf {

    /**
     * Vérifie que le fichier est un PDF lisible et compte ses pages.
     *
     * @return le nombre de pages, au moins 1
     * @throws FichierNonPdfException si ce n'est pas un PDF
     * @throws PdfIllisibleException  s'il est corrompu, chiffré ou sans page
     */
    int analyser(Path chemin);
}
