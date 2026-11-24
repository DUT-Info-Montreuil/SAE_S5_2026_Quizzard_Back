package fr.iutmontreuil.quizzard.features.seance.domain;

/**
 * Lien tracé par l'étudiant : {@code gauche} est l'identifiant de la paire dont il a choisi l'élément de gauche,
 * {@code droite} celui de la paire dont il a choisi l'élément de droite.
 */
public record Lien(Long gauche, Long droite) {

    /** Vrai si le lien relie les deux côtés d'une même paire. */
    public boolean estJuste() {
        return gauche != null && gauche.equals(droite);
    }
}
