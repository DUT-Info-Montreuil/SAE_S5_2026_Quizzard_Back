package fr.iutmontreuil.quizzard.features.cours.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionIntrouvable;

/** Le PDF de la version a été supprimé (UC20) ou n'est plus lisible sur le disque : 404. Code {@code FICHIER_INTROUVABLE}. */
public class FichierIntrouvableException extends ExceptionIntrouvable {

    public static final String CODE = "FICHIER_INTROUVABLE";

    public FichierIntrouvableException() {
        super("Le fichier de cette version n'est plus disponible.");
    }

    @Override
    public String code() {
        return CODE;
    }
}
