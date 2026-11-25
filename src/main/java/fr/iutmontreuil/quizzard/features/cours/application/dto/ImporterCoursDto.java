package fr.iutmontreuil.quizzard.features.cours.application.dto;

import java.io.InputStream;

/**
 * Données reçues pour importer un cours (UC2).
 *
 * @param titre            titre saisi, pas encore contrôlé
 * @param nomFichier       nom d'origine annoncé par le navigateur, peut-être {@code null}
 * @param contenu          octets du fichier ; l'appelant ferme le flux
 * @param idEnseignant     enseignant de la session de connexion, jamais lu dans la requête
 * @param confirmerDoublon vrai pour importer un PDF déjà présent dans un autre cours
 */
public record ImporterCoursDto(
        String titre,
        String nomFichier,
        InputStream contenu,
        Long idEnseignant,
        boolean confirmerDoublon) {

    @Override
    public String toString() {
        // Ni le titre ni le nom du fichier dans les logs.
        return "ImporterCoursDto[idEnseignant=" + idEnseignant + ", confirmerDoublon=" + confirmerDoublon + "]";
    }
}
