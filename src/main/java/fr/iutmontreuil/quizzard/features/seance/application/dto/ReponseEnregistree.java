package fr.iutmontreuil.quizzard.features.seance.application.dto;

import java.time.LocalDateTime;

/** Résultat d'une réponse enregistrée : ni points ni justesse avant la clôture de la question. */
public record ReponseEnregistree(Long idReponse, Long idQuestion, LocalDateTime horodatageServeur) {
}
