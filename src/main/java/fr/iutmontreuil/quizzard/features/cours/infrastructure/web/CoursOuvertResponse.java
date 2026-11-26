package fr.iutmontreuil.quizzard.features.cours.infrastructure.web;

import java.time.LocalDateTime;

/**
 * Cours ouvert dans l'éditeur (E3) : les champs de {@link CoursResponse}, plus l'identifiant de la dernière version,
 * dont l'éditeur a besoin pour y insérer des pages (UC3) et lancer un quiz (UC7), et {@code archive}.
 *
 * @param archive vrai si le cours est archivé : l'éditeur s'ouvre en lecture seule
 */
record CoursOuvertResponse(
        Long idCours,
        String titre,
        Long idVersion,
        int numeroVersion,
        int nbPages,
        int nbQuiz,
        LocalDateTime dateModification,
        boolean archive) {
}
