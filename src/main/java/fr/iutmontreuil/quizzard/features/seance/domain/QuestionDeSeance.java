package fr.iutmontreuil.quizzard.features.seance.domain;

import java.util.Comparator;
import java.util.List;

/**
 * Question jouée dans une séance (UC7) : son contenu attendu, sa durée effective, ses écrans intermédiaires et sa
 * place dans le cours présenté. Les réponses attendues restent sur le serveur jusqu'à la clôture de la question.
 *
 * @param dureeSec      durée de la question, sinon durée par défaut du quiz
 * @param pageAffichee  numéro de la page de quiz tel que l'éditeur le montre (E3), pour désigner une question
 *                      incomplète
 * @param propositions  propositions d'un QCM, dans leur rang d'affichage (vide sinon)
 * @param ecrans        écrans intermédiaires, dans leur ordre d'affichage
 */
public record QuestionDeSeance(
        Long idQuestion,
        TypeQuestion type,
        String enonce,
        int dureeSec,
        int pageAffichee,
        List<PropositionDeSeance> propositions,
        List<PaireDeSeance> paires,
        List<MotifDeSeance> motifs,
        List<EcranIntermediaire> ecrans) {

    public QuestionDeSeance {
        propositions = propositions.stream().sorted(Comparator.comparingInt(PropositionDeSeance::rang)).toList();
        paires = List.copyOf(paires);
        motifs = List.copyOf(motifs);
        ecrans = ecrans.stream().sorted(Comparator.comparingInt(EcranIntermediaire::rang)).toList();
    }

    /**
     * Vrai si la question peut être jouée : énoncé, durée et réponses attendues. QCM : 2 propositions et 1 bonne
     * réponse au moins ; points à relier : 2 paires au moins ; réponse libre : 1 motif au moins. Le barème est
     * toujours renseigné par la base.
     */
    public boolean estComplete() {
        return renseigne(enonce) && dureeSec > 0 && aSesReponsesAttendues();
    }

    /** Vrai pour un QCM qui accepte plusieurs cases cochées. */
    public boolean choixMultiple() {
        return propositions.stream().filter(PropositionDeSeance::estCorrecte).count() > 1;
    }

    private boolean aSesReponsesAttendues() {
        return switch (type) {
            case QCM -> propositions.size() >= 2
                    && propositions.stream().allMatch(p -> renseigne(p.libelle()))
                    && propositions.stream().anyMatch(PropositionDeSeance::estCorrecte);
            case POINTS_A_RELIER -> paires.size() >= 2
                    && paires.stream().allMatch(p -> renseigne(p.elementGauche()) && renseigne(p.elementDroit()));
            case REPONSE_LIBRE -> !motifs.isEmpty() && motifs.stream().allMatch(m -> renseigne(m.motif()));
        };
    }

    private static boolean renseigne(String texte) {
        return texte != null && !texte.isBlank();
    }
}
