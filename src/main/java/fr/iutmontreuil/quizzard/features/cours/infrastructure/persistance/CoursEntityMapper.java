package fr.iutmontreuil.quizzard.features.cours.infrastructure.persistance;

import java.util.List;

import fr.iutmontreuil.quizzard.features.cours.domain.CoursImporte;
import fr.iutmontreuil.quizzard.features.cours.domain.VersionImportee;

/** Conversion entre les entités JPA et le domaine, qui ne dépend d'aucun framework. */
final class CoursEntityMapper {

    private CoursEntityMapper() {
    }

    static CoursEntity versEntite(CoursImporte cours) {
        CoursEntity entite = new CoursEntity();
        entite.idCours = cours.idCours();
        entite.titre = cours.titre();
        entite.idEnseignant = cours.idEnseignant();
        entite.archive = cours.archive();
        entite.dateCreation = cours.dateCreation();
        entite.dateModification = cours.dateModification();
        return entite;
    }

    static VersionCoursEntity versEntite(VersionImportee version, Long idCours) {
        VersionCoursEntity entite = new VersionCoursEntity();
        entite.idVersion = version.idVersion();
        entite.numero = version.numero();
        entite.nomFichier = version.nomFichier();
        entite.cheminStockage = version.cheminStockage();
        entite.tailleOctets = version.tailleOctets();
        entite.nbPages = version.nbPages();
        entite.empreinte = version.empreinte();
        entite.dateImport = version.dateImport();
        entite.dateSuppressionFichier = version.dateSuppressionFichier();
        entite.idCours = idCours;
        return entite;
    }

    static VersionImportee versDomaine(VersionCoursEntity v) {
        return new VersionImportee(v.idVersion, v.numero, v.nomFichier, v.cheminStockage, v.tailleOctets, v.nbPages,
                v.empreinte, v.dateImport, v.dateSuppressionFichier);
    }

    static CoursImporte versDomaine(CoursEntity c, List<VersionCoursEntity> versions) {
        return new CoursImporte(c.idCours, c.titre, c.idEnseignant, c.archive, c.dateCreation, c.dateModification,
                versions.stream().map(CoursEntityMapper::versDomaine).toList());
    }
}
