package fr.iutmontreuil.quizzard.features.cours.application.dto;

import java.nio.file.Path;

/**
 * Fichier reçu, écrit sur le disque avant d'être contrôlé.
 *
 * @param tailleOctets taille réellement écrite : elle fait foi, pas celle annoncée par le client
 * @param empreinte    SHA-256 du contenu, 64 caractères hexadécimaux minuscules
 */
public record FichierTemporaire(Path chemin, long tailleOctets, String empreinte) {
}
