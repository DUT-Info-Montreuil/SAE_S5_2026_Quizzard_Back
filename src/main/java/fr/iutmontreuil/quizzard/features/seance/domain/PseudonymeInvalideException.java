package fr.iutmontreuil.quizzard.features.seance.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionDonneesInvalides;

/**
 * Pseudonyme absent, trop court ou trop long, ou contenant un caractère de contrôle (422). Dernier rempart
 * derrière la validation du contrôleur. Le message ne recopie jamais le pseudonyme reçu.
 */
public class PseudonymeInvalideException extends ExceptionDonneesInvalides {

    public PseudonymeInvalideException() {
        super("pseudonyme", "Ton pseudonyme doit faire de 2 à 20 caractères.");
    }
}
