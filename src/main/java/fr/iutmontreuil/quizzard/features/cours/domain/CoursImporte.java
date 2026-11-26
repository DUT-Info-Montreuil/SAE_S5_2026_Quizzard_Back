package fr.iutmontreuil.quizzard.features.cours.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cours créé par un import (UC2) ; le dossier de conception nomme cet agrégat {@code Cours}. Le cours en ligne est le
 * PDF conservé tel quel, avec les pages qu'on y insérera : le serveur ne produit pas d'autre format.
 *
 * @param idCours  {@code null} tant que le cours n'est pas enregistré
 * @param versions versions du cours, la première d'abord
 */
public record CoursImporte(
        Long idCours,
        String titre,
        Long idEnseignant,
        boolean archive,
        LocalDateTime dateCreation,
        LocalDateTime dateModification,
        List<VersionImportee> versions) {

    /** Taille de la colonne {@code cours.titre}. */
    public static final int LONGUEUR_MAX_TITRE = 200;

    public CoursImporte {
        versions = List.copyOf(versions);
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("un cours a au moins une version");
        }
    }

    /**
     * Nouveau cours avec sa première version, non encore enregistré.
     *
     * @throws TitreInvalideException   si le titre est absent, vide ou trop long
     * @throws IllegalArgumentException si un autre paramètre est nul (erreur de programmation)
     */
    public static CoursImporte creer(String titre, Long idEnseignant, VersionImportee premiereVersion,
                                     LocalDateTime maintenant) {
        String propre = nettoyerTitre(titre);
        if (idEnseignant == null || premiereVersion == null || maintenant == null) {
            throw new IllegalArgumentException("enseignant, première version et date sont obligatoires");
        }
        return new CoursImporte(null, propre, idEnseignant, false, maintenant, maintenant, List.of(premiereVersion));
    }

    /**
     * Titre débarrassé des espaces de début et de fin, de 1 à 200 caractères.
     *
     * @throws TitreInvalideException sinon, ou s'il contient un caractère nul que la base refuse
     */
    public static String nettoyerTitre(String titre) {
        String propre = titre == null ? "" : titre.strip();
        if (propre.isEmpty() || propre.indexOf('\0') >= 0
                || propre.codePointCount(0, propre.length()) > LONGUEUR_MAX_TITRE) {
            throw new TitreInvalideException();
        }
        return propre;
    }

    /** Version de numéro le plus élevé : celle que présente le cours. */
    public VersionImportee derniereVersion() {
        return versions.getLast();
    }

    @Override
    public String toString() {
        return "CoursImporte[idCours=" + idCours + ", versions=" + versions.size() + "]";
    }
}
