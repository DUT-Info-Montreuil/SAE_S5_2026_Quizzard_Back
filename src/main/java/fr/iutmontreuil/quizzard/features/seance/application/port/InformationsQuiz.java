package fr.iutmontreuil.quizzard.features.seance.application.port;

import fr.iutmontreuil.quizzard.features.seance.application.dto.TitresQuiz;

/**
 * Port propre à la feature {@code seance} pour lire les titres du quiz et du cours : elle ne dépend ainsi pas de
 * l'infrastructure des features {@code quiz} et {@code cours}.
 */
public interface InformationsQuiz {

    /** Titres du quiz joué dans cette séance et de son cours, pour l'écran « Rejoindre ». */
    TitresQuiz titresDeLaSession(Long idSession);
}
