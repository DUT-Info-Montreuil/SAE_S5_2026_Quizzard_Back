package fr.iutmontreuil.quizzard.features.seance.domain;

import java.util.ArrayList;
import java.util.List;

/** Quiz et questions prêts à l'emploi pour les tests de l'UC7, sans base ni Spring. */
public final class FabriqueLancement {

    public static final Long ID_QUIZ = 10L;
    public static final Long ID_ENSEIGNANT = 1L;
    public static final Long ID_VERSION = 100L;

    private FabriqueLancement() {
    }

    /** QCM complet : la proposition {@code idQuestion * 10} est la bonne. */
    public static QuestionDeSeance qcm(long idQuestion, int page, EcranIntermediaire... ecrans) {
        return new QuestionDeSeance(idQuestion, TypeQuestion.QCM, "Quel protocole ?", 20, page,
                List.of(new PropositionDeSeance(idQuestion * 10, "LDAP", true, 0),
                        new PropositionDeSeance(idQuestion * 10 + 1, "SMTP", false, 1)),
                List.of(), List.of(), List.of(ecrans));
    }

    public static QuestionDeSeance relier(long idQuestion, int page) {
        return new QuestionDeSeance(idQuestion, TypeQuestion.POINTS_A_RELIER, "Relie les ports", 30, page, List.of(),
                List.of(new PaireDeSeance(1L, "80", "HTTP"), new PaireDeSeance(2L, "53", "DNS"),
                        new PaireDeSeance(3L, "22", "SSH"), new PaireDeSeance(4L, "25", "SMTP")),
                List.of(), List.of());
    }

    public static QuestionDeSeance libre(long idQuestion, int page) {
        return new QuestionDeSeance(idQuestion, TypeQuestion.REPONSE_LIBRE, "Quel protocole chiffre le web ?", 15,
                page, List.of(), List.of(),
                List.of(new MotifDeSeance("https", false), new MotifDeSeance("tls|ssl", true)), List.of());
    }

    /** QCM sans énoncé : question incomplète. */
    public static QuestionDeSeance incomplete(long idQuestion, int page) {
        QuestionDeSeance q = qcm(idQuestion, page);
        return new QuestionDeSeance(idQuestion, q.type(), "  ", q.dureeSec(), page, q.propositions(), q.paires(),
                q.motifs(), q.ecrans());
    }

    public static Quiz quiz(QuestionDeSeance... questions) {
        return quiz(false, false, null, questions);
    }

    public static Quiz quizNote(QuestionDeSeance... questions) {
        return quiz(true, false, null, questions);
    }

    public static Quiz quiz(boolean note, boolean passageAutomatique, Integer delai, QuestionDeSeance... questions) {
        return new Quiz(ID_QUIZ, ID_ENSEIGNANT, "Quiz 1", "Réseaux", note, passageAutomatique, delai, ID_VERSION,
                new ArrayList<>(List.of(questions)));
    }
}
