package fr.iutmontreuil.quizzard.features.cours.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionTypeNonSupporte;

/** Le contenu reçu n'est pas un PDF, quels que soient son nom et le type annoncé : 415 {@code FICHIER_NON_PDF}. */
public class FichierNonPdfException extends ExceptionTypeNonSupporte {

    public FichierNonPdfException() {
        super("Le fichier n'est pas un PDF.");
    }

    @Override
    public String code() {
        return "FICHIER_NON_PDF";
    }
}
