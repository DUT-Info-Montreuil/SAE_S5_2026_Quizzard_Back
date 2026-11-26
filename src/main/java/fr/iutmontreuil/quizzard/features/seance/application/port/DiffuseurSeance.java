package fr.iutmontreuil.quizzard.features.seance.application.port;

import java.util.List;

import fr.iutmontreuil.quizzard.features.seance.application.dto.CorrectionQuestion;
import fr.iutmontreuil.quizzard.features.seance.application.dto.EcranAAfficher;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QuestionProjetee;
import fr.iutmontreuil.quizzard.features.seance.application.dto.ResultatPersonnel;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;

/**
 * Événements temps réel du déroulement d'une séance (UC7) ; le dossier de conception nomme ce port
 * {@code DiffuseurTempsReel}. Tout envoi demandé dans une transaction n'a lieu qu'après sa validation : un
 * changement annulé n'est jamais annoncé.
 */
public interface DiffuseurSeance {

    /**
     * Ouvre le canal temps réel propre à la séance et l'annonce une fois la transaction validée.
     *
     * @throws RuntimeException si le canal ne peut pas être ouvert : la transaction est annulée
     */
    void ouvrirCanal(SeanceAnimee seance);

    /** {@code question_demarree} : à tous, sans les bonnes réponses. */
    void annoncerQuestion(String codeAcces, QuestionProjetee question);

    /** {@code question_close} : la correction à tous, et à chaque étudiant son résultat. */
    void annoncerCorrection(String codeAcces, CorrectionQuestion correction, List<ResultatPersonnel> resultats);

    /** {@code ecran_intermediaire} : à l'écran projeté. */
    void afficherEcran(String codeAcces, EcranAAfficher ecran);

    /** {@code session_terminee} : à tous. */
    void annoncerFin(String codeAcces, int position);
}
