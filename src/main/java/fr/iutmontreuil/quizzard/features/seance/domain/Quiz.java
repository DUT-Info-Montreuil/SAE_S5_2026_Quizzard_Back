package fr.iutmontreuil.quizzard.features.seance.domain;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Quiz à lancer, ou en cours de séance (UC7), avec les questions de la version du cours présentée, dans l'ordre de
 * leurs pages. La question de rang 1 est la première page de quiz rencontrée dans le cours.
 *
 * @param idEnseignant        enseignant propriétaire du cours du quiz
 * @param delaiPassageAutoSec attente avant la question suivante en passage automatique, ou {@code null}
 * @param idVersion           version du cours présentée, ou {@code null} si le cours n'en a pas
 */
public record Quiz(
        Long idQuiz,
        Long idEnseignant,
        String titre,
        String titreCours,
        boolean estNote,
        boolean passageAutomatique,
        Integer delaiPassageAutoSec,
        Long idVersion,
        List<QuestionDeSeance> questions) {

    /** Attente avant la question suivante en passage automatique quand aucun délai n'est réglé (à valider). */
    public static final int DELAI_PASSAGE_PAR_DEFAUT_SEC = 5;

    public Quiz {
        questions = List.copyOf(questions);
    }

    /** @throws AccesQuizRefuseException si le quiz n'appartient pas à cet enseignant */
    public void verifierPropriete(Long idEnseignantConnecte) {
        if (!Objects.equals(idEnseignant, idEnseignantConnecte)) {
            throw new AccesQuizRefuseException();
        }
    }

    /**
     * Le quiz est « prêt » s'il a au moins une question à présenter et qu'aucune n'est incomplète.
     *
     * @throws QuizIncompletException avec les pages des questions incomplètes
     */
    public void verifierPretALancer() {
        if (questions.isEmpty()) {
            throw QuizIncompletException.sansQuestion();
        }
        List<Integer> incompletes = questions.stream()
                .filter(question -> !question.estComplete())
                .map(QuestionDeSeance::pageAffichee)
                .toList();
        if (!incompletes.isEmpty()) {
            throw QuizIncompletException.questionsIncompletes(incompletes);
        }
    }

    public int nbQuestions() {
        return questions.size();
    }

    /** Question de rang {@code position} (1 pour la première), vide si le rang sort du quiz. */
    public Optional<QuestionDeSeance> question(int position) {
        return position >= 1 && position <= questions.size()
                ? Optional.of(questions.get(position - 1))
                : Optional.empty();
    }

    /**
     * Attente entre la clôture d'une question et l'ouverture de la suivante en passage automatique : le dernier écran
     * intermédiaire de la question, puis le délai réglé sur le quiz (DESIGN.md §4).
     */
    public Duration attenteAvantSuivante(QuestionDeSeance question) {
        int dernierEcran = question.ecrans().stream()
                .mapToInt(EcranIntermediaire::delaiApresClotureSec)
                .max()
                .orElse(0);
        int delai = delaiPassageAutoSec == null ? DELAI_PASSAGE_PAR_DEFAUT_SEC : delaiPassageAutoSec;
        return Duration.ofSeconds((long) dernierEcran + delai);
    }
}
