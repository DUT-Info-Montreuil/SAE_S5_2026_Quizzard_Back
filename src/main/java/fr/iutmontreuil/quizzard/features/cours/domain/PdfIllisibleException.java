package fr.iutmontreuil.quizzard.features.cours.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionDonneesInvalides;

/** PDF corrompu, chiffré ou sans page : 422 {@code PDF_ILLISIBLE}, sur le champ {@code fichier}. */
public class PdfIllisibleException extends ExceptionDonneesInvalides {

    public PdfIllisibleException() {
        super("fichier", "Le PDF est illisible ou protégé par un mot de passe.");
    }

    @Override
    public String code() {
        return "PDF_ILLISIBLE";
    }
}
