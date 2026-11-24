package fr.iutmontreuil.quizzard.features.seance.application.dto;

import java.util.List;

import fr.iutmontreuil.quizzard.features.seance.domain.Lien;

/**
 * Réponse reçue d'un étudiant. Le participant est retrouvé par son jeton, jamais par un identifiant envoyé par le
 * client ; aucun horodatage du client n'est lu.
 *
 * @param codeAcces      code d'accès de la séance (adresse de la route)
 * @param jeton          jeton de participant (en-tête X-Jeton-Participant)
 * @param idQuestion     question à laquelle l'étudiant répond
 * @param idPropositions propositions cochées (QCM)
 * @param liens          liens tracés (points à relier)
 * @param texteLibre     texte saisi (réponse libre)
 */
public record RepondreQuestionDto(
        String codeAcces,
        String jeton,
        Long idQuestion,
        List<Long> idPropositions,
        List<Lien> liens,
        String texteLibre) {
}
