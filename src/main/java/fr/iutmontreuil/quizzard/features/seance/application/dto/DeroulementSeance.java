package fr.iutmontreuil.quizzard.features.seance.application.dto;

import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;

/**
 * État d'une séance pour l'écran E5 de l'enseignant : au chargement, après une coupure réseau et après chaque
 * commande. L'écran est projeté : la correction n'est présente qu'une fois la question close.
 *
 * @param lienAcces      lien des étudiants, ou {@code null} si la séance est terminée (il ne vaut plus)
 * @param qrCode         QR code du lien, ou {@code null} si la séance est terminée
 * @param connectes      nombre de participants entrés dans la séance
 * @param question       question de la position courante, sans réponses attendues, ou {@code null} avant la première
 * @param tempsRestantMs temps restant de la question ouverte, ou {@code null} s'il n'y en a pas
 * @param reponses       réponses reçues à la question courante, ou {@code null} avant la première
 * @param correction     correction de la question courante, ou {@code null} tant qu'elle n'est pas close
 */
public record DeroulementSeance(
        SeanceAnimee seance,
        String titre,
        int nbQuestions,
        String lienAcces,
        QrCode qrCode,
        int connectes,
        QuestionProjetee question,
        Long tempsRestantMs,
        RepartitionReponses reponses,
        CorrectionQuestion correction) {

    @Override
    public String toString() {
        return "DeroulementSeance[idSession=" + seance.idSession() + ", etat=" + seance.etat() + ", position="
                + seance.positionCourante() + "]";
    }
}
