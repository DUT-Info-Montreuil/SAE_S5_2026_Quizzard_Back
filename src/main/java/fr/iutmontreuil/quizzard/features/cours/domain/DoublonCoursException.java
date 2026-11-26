package fr.iutmontreuil.quizzard.features.cours.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflit;

/**
 * L'enseignant a déjà importé ce PDF dans un cours non archivé : 409 {@code DOUBLON_COURS}. Le front propose de
 * renvoyer le fichier avec {@code confirmerDoublon}.
 */
public class DoublonCoursException extends ExceptionConflit {

    public DoublonCoursException(String titreCoursExistant) {
        super("Vous avez déjà importé ce PDF dans le cours « " + titreCoursExistant + " ». Importer quand même ?");
    }

    @Override
    public String code() {
        return "DOUBLON_COURS";
    }
}
