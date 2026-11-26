package fr.iutmontreuil.quizzard.features.seance.infrastructure.format;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Question sans réponses attendues, sur l'écran projeté et sur les appareils des étudiants. Même forme que celle
 * que l'UC9 renvoie à l'étudiant ({@code GET .../etat}) et que le front sait déjà lire. Champs propres au type :
 * {@code propositions} et {@code choixMultiple} (QCM), {@code elementsGauche} et {@code elementsDroite} (points à
 * relier) ; absents pour les autres types.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionJson(
        Long idQuestion,
        int position,
        String type,
        String enonce,
        int dureeSec,
        OffsetDateTime echeance,
        List<Proposition> propositions,
        Boolean choixMultiple,
        List<Element> elementsGauche,
        List<Element> elementsDroite) {

    public record Proposition(Long idProposition, String libelle) {
    }

    /** Élément à relier, désigné par l'identifiant de sa paire. */
    public record Element(Long id, String libelle) {
    }
}
