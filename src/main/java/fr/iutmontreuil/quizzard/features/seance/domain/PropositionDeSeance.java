package fr.iutmontreuil.quizzard.features.seance.domain;

/** Proposition d'un QCM joué en séance. {@code estCorrecte} reste sur le serveur jusqu'à la clôture. */
public record PropositionDeSeance(Long idProposition, String libelle, boolean estCorrecte, int rang) {
}
