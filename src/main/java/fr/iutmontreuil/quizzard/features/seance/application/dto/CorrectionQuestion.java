package fr.iutmontreuil.quizzard.features.seance.application.dto;

import java.util.Comparator;
import java.util.List;

import fr.iutmontreuil.quizzard.features.seance.domain.MotifDeSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.PaireDeSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.PropositionDeSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionDeSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.TypeQuestion;

/**
 * Correction d'une question close (événement {@code question_close} du dossier) : les réponses attendues et la
 * répartition. Cet objet ne doit jamais exister avant la clôture.
 *
 * @param bonnesPropositions propositions correctes d'un QCM
 * @param paires             associations attendues (points à relier)
 * @param reponsesAcceptees  motifs simples d'une réponse libre ; les expressions régulières ne sont pas montrées
 * @param repartition        nombre de réponses par proposition, toutes les propositions comprises (vide hors QCM)
 */
public record CorrectionQuestion(
        Long idQuestion,
        int position,
        TypeQuestion type,
        List<Long> bonnesPropositions,
        List<Association> paires,
        List<String> reponsesAcceptees,
        int total,
        List<ParProposition> repartition) {

    /**
     * Association attendue : les deux côtés d'une paire portent son identifiant, comme dans la réponse d'un
     * étudiant.
     */
    public record Association(Long gauche, Long droite, String libelleGauche, String libelleDroite) {
    }

    public record ParProposition(Long idProposition, int nombre) {
    }

    public static CorrectionQuestion de(QuestionDeSeance question, int position, RepartitionReponses reponses) {
        List<Long> bonnes = question.propositions().stream()
                .filter(PropositionDeSeance::estCorrecte)
                .map(PropositionDeSeance::idProposition)
                .toList();
        List<Association> paires = question.paires().stream()
                .sorted(Comparator.comparing(PaireDeSeance::idPaire))
                .map(p -> new Association(p.idPaire(), p.idPaire(), p.elementGauche(), p.elementDroit()))
                .toList();
        List<String> acceptees = question.motifs().stream()
                .filter(m -> !m.expressionReguliere())
                .map(MotifDeSeance::motif)
                .toList();
        List<ParProposition> repartition = question.propositions().stream()
                .map(p -> new ParProposition(p.idProposition(),
                        reponses.parProposition().getOrDefault(p.idProposition(), 0)))
                .toList();
        boolean qcm = question.type() == TypeQuestion.QCM;
        return new CorrectionQuestion(question.idQuestion(), position, question.type(),
                qcm ? bonnes : List.of(),
                question.type() == TypeQuestion.POINTS_A_RELIER ? paires : List.of(),
                question.type() == TypeQuestion.REPONSE_LIBRE ? acceptees : List.of(),
                reponses.total(),
                qcm ? repartition : List.of());
    }

    /** La bonne réponse en texte, pour l'écran de résultat de l'étudiant. */
    public static List<String> bonneReponseEnTexte(QuestionDeSeance question) {
        return switch (question.type()) {
            case QCM -> question.propositions().stream()
                    .filter(PropositionDeSeance::estCorrecte)
                    .map(PropositionDeSeance::libelle)
                    .toList();
            case POINTS_A_RELIER -> question.paires().stream()
                    .sorted(Comparator.comparing(PaireDeSeance::idPaire))
                    .map(p -> p.elementGauche() + " → " + p.elementDroit())
                    .toList();
            case REPONSE_LIBRE -> question.motifs().stream()
                    .filter(m -> !m.expressionReguliere())
                    .map(MotifDeSeance::motif)
                    .toList();
        };
    }
}
