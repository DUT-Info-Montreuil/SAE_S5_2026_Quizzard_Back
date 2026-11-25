package fr.iutmontreuil.quizzard.features.cours.infrastructure.web;

import fr.iutmontreuil.quizzard.features.cours.domain.CoursImporte;
import fr.iutmontreuil.quizzard.features.cours.domain.VersionImportee;

/** Conversion d'un cours en JSON pour le navigateur de l'enseignant. */
final class CoursMapper {

    private CoursMapper() {
    }

    /** Cours qui vient d'être importé : il n'a pas encore de quiz. */
    static CoursResponse versReponse(CoursImporte cours) {
        VersionImportee version = cours.derniereVersion();
        return new CoursResponse(cours.idCours(), cours.titre(), version.numero(), version.nbPages(), 0,
                cours.dateModification());
    }
}
