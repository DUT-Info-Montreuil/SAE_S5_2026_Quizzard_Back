package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import java.time.OffsetDateTime;

/** Accusé de réception d'une réponse : ni points ni justesse avant la clôture de la question. */
record ReponseEnregistreeResponse(Long idQuestion, OffsetDateTime horodatageServeur) {
}
