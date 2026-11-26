package fr.iutmontreuil.quizzard.features.cours.domain;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Version d'un cours créée par un import (UC2) ; le dossier de conception la nomme {@code VersionCours}. Elle décrit
 * le fichier PDF rangé sur le serveur.
 *
 * @param idVersion              {@code null} tant que la version n'est pas enregistrée
 * @param nomFichier             nom d'origine du fichier, sans dossier, 255 caractères au plus
 * @param cheminStockage         chemin relatif au dossier de stockage, ou {@code null} si le fichier a été supprimé
 * @param empreinte              SHA-256 du fichier, 64 caractères hexadécimaux minuscules
 * @param dateSuppressionFichier date de suppression du fichier par l'administrateur, ou {@code null}
 */
public record VersionImportee(
        Long idVersion,
        int numero,
        String nomFichier,
        String cheminStockage,
        long tailleOctets,
        int nbPages,
        String empreinte,
        LocalDateTime dateImport,
        LocalDateTime dateSuppressionFichier) {

    /** Taille de la colonne {@code version_cours.nom_fichier}. */
    public static final int LONGUEUR_MAX_NOM = 255;
    static final String NOM_PAR_DEFAUT = "cours.pdf";

    private static final Pattern EMPREINTE = Pattern.compile("[0-9a-f]{64}");

    /**
     * Version 1 du cours, à partir des informations du fichier contrôlé, non encore enregistrée.
     *
     * @throws IllegalArgumentException si le nombre de pages est inférieur à 1, si l'empreinte n'a pas 64 caractères
     *                                  hexadécimaux ou si le chemin ou la date manque : erreur de programmation, car
     *                                  ces valeurs viennent de l'analyse du fichier
     */
    public static VersionImportee premiere(String nomFichier, String cheminStockage, long tailleOctets, int nbPages,
                                           String empreinte, LocalDateTime maintenant) {
        if (nbPages < 1) {
            throw new IllegalArgumentException("nbPages doit être au moins 1 : " + nbPages);
        }
        if (empreinte == null || !EMPREINTE.matcher(empreinte).matches()) {
            throw new IllegalArgumentException("empreinte SHA-256 attendue : 64 caractères hexadécimaux");
        }
        if (cheminStockage == null || cheminStockage.isBlank() || tailleOctets < 0 || maintenant == null) {
            throw new IllegalArgumentException("chemin de stockage, taille et date sont obligatoires");
        }
        return new VersionImportee(null, 1, nomSansDossier(nomFichier), cheminStockage, tailleOctets, nbPages,
                empreinte, maintenant, null);
    }

    /** La même version avec l'identifiant attribué par la base. */
    public VersionImportee avecIdentifiant(Long identifiant) {
        return new VersionImportee(identifiant, numero, nomFichier, cheminStockage, tailleOctets, nbPages, empreinte,
                dateImport, dateSuppressionFichier);
    }

    /**
     * Nom donné par le navigateur, sans les dossiers qu'il pourrait contenir et sans caractère de contrôle. Il sert
     * seulement à l'affichage : le fichier est rangé sous un nom tiré au hasard.
     */
    private static String nomSansDossier(String nom) {
        if (nom == null) {
            return NOM_PAR_DEFAUT;
        }
        String sansDossier = nom.substring(Math.max(nom.lastIndexOf('/'), nom.lastIndexOf('\\')) + 1);
        String propre = sansDossier.replaceAll("\\p{Cntrl}", "").strip();
        if (propre.isEmpty()) {
            return NOM_PAR_DEFAUT;
        }
        if (propre.codePointCount(0, propre.length()) <= LONGUEUR_MAX_NOM) {
            return propre;
        }
        return propre.substring(0, propre.offsetByCodePoints(0, LONGUEUR_MAX_NOM));
    }

    @Override
    public String toString() {
        // Le nom du fichier peut désigner une personne : il n'apparaît pas dans les logs.
        return "VersionImportee[idVersion=" + idVersion + ", numero=" + numero + ", nbPages=" + nbPages + "]";
    }
}
