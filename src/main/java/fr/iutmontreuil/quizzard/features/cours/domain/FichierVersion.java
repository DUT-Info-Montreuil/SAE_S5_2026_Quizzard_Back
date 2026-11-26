package fr.iutmontreuil.quizzard.features.cours.domain;

/**
 * Fichier PDF d'une version du cours.
 *
 * @param cheminStockage chemin relatif dans le dossier de stockage, ou {@code null} si l'administrateur a supprimé le
 *                       fichier (UC20 ; la version reste, sans fichier)
 */
public record FichierVersion(String cheminStockage) {

    public boolean estSupprime() {
        return cheminStockage == null;
    }
}
