package fr.iutmontreuil.quizzard.features.seance.application.dto;

import java.time.LocalDateTime;
import java.util.List;

import fr.iutmontreuil.quizzard.features.seance.domain.TypeQuestion;

/**
 * Question telle que l'étudiant la voit pendant qu'elle est ouverte : sans aucune réponse attendue (ni
 * {@code estCorrecte}, ni paires, ni motifs). Pour les points à relier, les deux colonnes sont mélangées
 * séparément.
 *
 * @param propositions   propositions d'un QCM, dans leur rang d'affichage (vide sinon)
 * @param choixMultiple  vrai si plusieurs propositions sont correctes (QCM seulement, faux sinon)
 * @param elementsGauche éléments de gauche (points à relier), désignés par l'identifiant de leur paire
 * @param elementsDroite éléments de droite (points à relier), désignés par l'identifiant de leur paire
 */
public record QuestionPourEtudiant(
        Long idQuestion,
        int position,
        TypeQuestion type,
        String enonce,
        int dureeSec,
        LocalDateTime echeance,
        List<Choix> propositions,
        boolean choixMultiple,
        List<Choix> elementsGauche,
        List<Choix> elementsDroite) {

    /** Élément affichable : un identifiant et un libellé. */
    public record Choix(Long id, String libelle) {
    }
}
