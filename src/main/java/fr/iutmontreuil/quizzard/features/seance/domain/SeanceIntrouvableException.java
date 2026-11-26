package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionIntrouvable;

/**
 * La séance demandée par l'enseignant n'existe pas : 404 {@code SEANCE_INTROUVABLE}. Les routes des étudiants ont
 * leur propre exception, avec un message qui s'adresse à eux.
 */
public class SeanceIntrouvableException extends ExceptionIntrouvable {

    public SeanceIntrouvableException() {
        this("Cette séance n'existe pas ou a été supprimée.");
    }

    private SeanceIntrouvableException(String message) {
        super(message);
    }

    /** L'enseignant n'a aucune séance non terminée. */
    public static SeanceIntrouvableException aucuneOuverte() {
        return new SeanceIntrouvableException("Aucune séance n'est ouverte.");
    }

    @Override
    public String code() {
        return "SEANCE_INTROUVABLE";
    }
}
