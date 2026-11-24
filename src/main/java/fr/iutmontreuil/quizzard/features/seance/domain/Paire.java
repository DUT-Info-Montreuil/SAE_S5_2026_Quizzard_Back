package fr.iutmontreuil.quizzard.features.seance.domain;

/**
 * Paire attendue d'une question « points à relier ». L'étudiant voit deux colonnes : les éléments de gauche et
 * les éléments de droite, chacun désigné par l'identifiant de sa paire. Un lien est juste quand il relie les deux
 * côtés de la même paire.
 */
public record Paire(Long idPaire, String elementGauche, String elementDroit) {
}
