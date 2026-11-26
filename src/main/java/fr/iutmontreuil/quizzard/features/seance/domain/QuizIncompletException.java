package fr.iutmontreuil.quizzard.features.seance.domain;

import java.util.List;
import java.util.stream.Collectors;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionDonneesInvalides;

/**
 * Le quiz n'est pas prêt : aucune question à présenter, ou au moins une question incomplète. 422
 * {@code QUIZ_INCOMPLET}, avec les pages à compléter dans le message.
 */
public class QuizIncompletException extends ExceptionDonneesInvalides {

    private final List<Integer> pages;

    private QuizIncompletException(String message, List<Integer> pages) {
        super(message);
        this.pages = List.copyOf(pages);
    }

    public static QuizIncompletException sansQuestion() {
        return new QuizIncompletException(
                "Le quiz ne contient aucune question à présenter. Insérez une page de quiz avant de lancer.",
                List.of());
    }

    /** @param pages numéros, tels que l'éditeur les montre, des pages dont la question est incomplète */
    public static QuizIncompletException questionsIncompletes(List<Integer> pages) {
        String numeros = pages.stream().map(String::valueOf).collect(Collectors.joining(", "));
        String message = pages.size() == 1
                ? "Le quiz contient une question incomplète (page " + numeros + "). Complétez-la avant de lancer."
                : "Le quiz contient des questions incomplètes (pages " + numeros + "). Complétez-les avant de lancer.";
        return new QuizIncompletException(message, pages);
    }

    /** Pages à compléter ; vide si le quiz n'a aucune question. */
    public List<Integer> pages() {
        return pages;
    }

    @Override
    public String code() {
        return "QUIZ_INCOMPLET";
    }
}
