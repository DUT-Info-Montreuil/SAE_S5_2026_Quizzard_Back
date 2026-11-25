package fr.iutmontreuil.quizzard.features.cours.application.dto;

import java.time.LocalDateTime;

/**
 * Cours tel que le lit son enseignant sur « Mes cours » (E2) et à l'ouverture de l'éditeur (E3), avec sa dernière
 * version.
 *
 * @param idVersion        dernière version : celle de numéro le plus élevé
 * @param numeroVersion    numéro de cette version
 * @param nbPages          pages du PDF de cette version
 * @param nbQuiz           quiz non archivés du cours ; un quiz appartient au cours, pas à une version
 * @param dateModification dernière modification du cours
 */
public record CoursConsulte(
        Long idCours,
        String titre,
        Long idVersion,
        int numeroVersion,
        int nbPages,
        int nbQuiz,
        LocalDateTime dateModification) {

    @Override
    public String toString() {
        // Le titre peut désigner une personne : il n'apparaît pas dans les logs.
        return "CoursConsulte[idCours=" + idCours + ", idVersion=" + idVersion + "]";
    }
}
