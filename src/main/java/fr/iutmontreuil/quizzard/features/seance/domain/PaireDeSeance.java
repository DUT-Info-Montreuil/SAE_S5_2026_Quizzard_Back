package fr.iutmontreuil.quizzard.features.seance.domain;

/** Paire attendue d'une question « points à relier » jouée en séance. L'association reste secrète jusqu'à la clôture. */
public record PaireDeSeance(Long idPaire, String elementGauche, String elementDroit) {
}
