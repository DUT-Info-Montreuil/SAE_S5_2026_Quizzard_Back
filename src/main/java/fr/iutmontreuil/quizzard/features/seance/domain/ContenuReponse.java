package fr.iutmontreuil.quizzard.features.seance.domain;

import java.util.List;

/**
 * Contenu d'une réponse tel que reçu de l'étudiant : un seul des trois champs sert, selon le type de la question.
 * Objet du domaine (et non le DTO de la couche application) pour que le domaine ne dépende de rien.
 *
 * @param idPropositions propositions cochées (QCM)
 * @param liens          liens tracés (points à relier)
 * @param texteLibre     texte saisi (réponse libre)
 */
public record ContenuReponse(List<Long> idPropositions, List<Lien> liens, String texteLibre) {
}
