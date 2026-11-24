package fr.iutmontreuil.quizzard.features.seance.infrastructure.tempsreel;

import java.time.OffsetDateTime;

/** Accusé de réception sur {@code /user/queue/reponses} : ni points ni justesse avant la clôture. */
record ReponseEnregistreeMessage(String requestId, Long idQuestion, OffsetDateTime horodatageServeur) {
}
