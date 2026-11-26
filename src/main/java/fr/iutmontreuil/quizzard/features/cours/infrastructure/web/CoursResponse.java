package fr.iutmontreuil.quizzard.features.cours.infrastructure.web;

import java.time.LocalDateTime;

/**
 * Cours affiché dans « Mes cours » (E2).
 *
 * @param numeroVersion    numéro de la dernière version
 * @param nbPages          pages du PDF de cette version
 * @param dateModification heure du serveur
 */
record CoursResponse(
        Long idCours,
        String titre,
        int numeroVersion,
        int nbPages,
        int nbQuiz,
        LocalDateTime dateModification) {
}
