package fr.iutmontreuil.quizzard.features.seance.domain;

/**
 * Applique un motif à un texte déjà normalisé. Interface du domaine, implémentée en infrastructure par un moteur
 * d'expressions régulières à temps linéaire (DESIGN.md §5) : le domaine ne dépend d'aucune bibliothèque.
 */
@FunctionalInterface
public interface VerificateurMotif {

    /** Vrai si le motif accepte le texte normalisé. Un motif invalide n'accepte rien. */
    boolean accepte(Motif motif, String texteNormalise);
}
