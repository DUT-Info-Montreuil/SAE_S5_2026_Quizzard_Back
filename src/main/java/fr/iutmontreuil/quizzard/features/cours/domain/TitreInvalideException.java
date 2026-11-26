package fr.iutmontreuil.quizzard.features.cours.domain;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionDonneesInvalides;

/** Titre absent, vide ou de plus de 200 caractères : 422 {@code TITRE_INVALIDE}, sur le champ {@code titre}. */
public class TitreInvalideException extends ExceptionDonneesInvalides {

    public TitreInvalideException() {
        super("titre", "Le titre est obligatoire (200 caractères au plus).");
    }

    @Override
    public String code() {
        return "TITRE_INVALIDE";
    }
}
