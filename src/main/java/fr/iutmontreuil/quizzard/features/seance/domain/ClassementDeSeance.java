package fr.iutmontreuil.quizzard.features.seance.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Rangs des participants après la clôture d'une question (DESIGN.md §7) : score décroissant ; à égalité, la somme des
 * temps de réponse la plus faible passe devant (à valider), puis l'ordre d'arrivée. Seul le rang de chaque étudiant
 * lui est envoyé ; le classement projeté relève de la feature {@code classement}.
 */
public final class ClassementDeSeance {

    private ClassementDeSeance() {
    }

    /**
     * @param rang      1 pour le premier
     * @param evolution places gagnées (positif) ou perdues (négatif) depuis la question précédente
     */
    public record Rang(int rang, int evolution) {
    }

    /** Position d'un participant dans un classement, avant ou après la question. */
    private record Ligne(Long idParticipant, int score, long tempsMs, LocalDateTime dateEntree) {
    }

    private static final Comparator<Ligne> ORDRE = Comparator.comparingInt(Ligne::score).reversed()
            .thenComparingLong(Ligne::tempsMs)
            .thenComparing(Ligne::dateEntree)
            .thenComparing(Ligne::idParticipant);

    /**
     * Rang de chaque participant, par identifiant.
     *
     * @param premiereQuestion vrai pour la première question : il n'y a pas de classement précédent, l'évolution vaut 0
     */
    public static Map<Long, Rang> apresQuestion(List<ScoreParticipant> scores, boolean premiereQuestion) {
        Map<Long, Integer> apres = rangs(scores,
                s -> new Ligne(s.idParticipant(), s.scoreTotal(), s.tempsTotalMs(), s.dateEntree()));
        Map<Long, Integer> avant = premiereQuestion ? apres : rangs(scores,
                s -> new Ligne(s.idParticipant(), s.scoreTotal() - s.pointsQuestion(),
                        s.tempsTotalMs() - s.tempsQuestionMs(), s.dateEntree()));
        Map<Long, Rang> resultat = new HashMap<>();
        apres.forEach((id, rang) -> resultat.put(id, new Rang(rang, avant.get(id) - rang)));
        return resultat;
    }

    private static Map<Long, Integer> rangs(List<ScoreParticipant> scores, Function<ScoreParticipant, Ligne> ligne) {
        List<Ligne> tri = scores.stream().map(ligne).sorted(ORDRE).toList();
        Map<Long, Integer> rangs = new HashMap<>();
        for (int i = 0; i < tri.size(); i++) {
            rangs.put(tri.get(i).idParticipant(), i + 1);
        }
        return rangs;
    }
}
