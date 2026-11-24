package fr.iutmontreuil.quizzard.features.seance.domain;

/** Proposition d'un QCM. {@code estCorrecte} ne quitte jamais le serveur avant la clôture de la question. */
public record Proposition(Long idProposition, String libelle, boolean estCorrecte, int rang) {
}
