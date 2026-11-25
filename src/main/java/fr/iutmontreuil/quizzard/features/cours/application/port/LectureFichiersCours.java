package fr.iutmontreuil.quizzard.features.cours.application.port;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Lecture des PDF rangés par l'import (UC2), dans le dossier de stockage, hors de l'espace servi au public. Un PDF
 * n'est jamais servi en accès direct : il passe par une route qui vérifie le propriétaire.
 */
public interface LectureFichiersCours {

    /**
     * Fichier lisible pour ce chemin enregistré en base ; vide s'il n'existe pas, n'est pas lisible ou sort du dossier
     * de stockage (chemin absolu, {@code ../}).
     */
    Optional<Path> fichierLisible(String cheminStockage);
}
