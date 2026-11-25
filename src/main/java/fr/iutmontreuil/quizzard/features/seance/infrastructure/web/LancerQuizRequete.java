package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

/**
 * Corps facultatif de {@code POST /api/quiz/{idQuiz}/sessions}.
 *
 * @param autoriserIdentiteNonVerifiee faux par défaut ; utile seulement pour un quiz noté si l'annuaire est
 *                                     indisponible
 */
record LancerQuizRequete(Boolean autoriserIdentiteNonVerifiee) {

    static boolean autorise(LancerQuizRequete requete) {
        return requete != null && Boolean.TRUE.equals(requete.autoriserIdentiteNonVerifiee());
    }
}
