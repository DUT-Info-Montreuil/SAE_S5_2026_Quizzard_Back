package fr.iutmontreuil.quizzard.features.cours.application.port;

import java.util.Optional;

import fr.iutmontreuil.quizzard.features.cours.domain.CoursImporte;

/** Cours créés par un import (UC2) ; le dossier de conception nomme ce port {@code CoursRepository}. */
public interface ImportCoursRepository {

    /**
     * Verrouille l'enseignant jusqu'à la fin de la transaction : deux imports simultanés s'exécutent l'un après
     * l'autre, le second voit le cours créé par le premier.
     */
    void verrouillerImports(Long idEnseignant);

    /** Le cours non archivé de l'enseignant qui contient déjà ce PDF, s'il existe (le plus récent). */
    Optional<CoursImporte> trouverParEmpreinte(Long idEnseignant, String empreinte);

    /**
     * Enregistre un nouveau cours et sa version, en forçant l'écriture pour qu'une erreur soit détectée avant la fin
     * du cas d'usage.
     *
     * @return le cours et sa version avec leurs identifiants
     */
    CoursImporte enregistrer(CoursImporte cours);
}
