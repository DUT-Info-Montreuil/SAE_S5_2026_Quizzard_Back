package fr.iutmontreuil.quizzard.features.seance.application.dto;

import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;

/**
 * Résultat du lancement : la séance enregistrée, son lien d'accès et son QR code. Le lien contient le code d'accès :
 * {@code toString()} ne le montre pas.
 *
 * @param titre titre du quiz lancé
 */
public record SessionLancee(SeanceAnimee seance, String titre, int nbQuestions, String lienAcces, QrCode qrCode) {

    @Override
    public String toString() {
        return "SessionLancee[idSession=" + seance.idSession() + ", nbQuestions=" + nbQuestions + "]";
    }
}
