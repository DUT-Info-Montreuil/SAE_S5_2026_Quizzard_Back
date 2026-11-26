package fr.iutmontreuil.quizzard.features.seance.application;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionConflit;

/**
 * Quiz noté alors que l'annuaire de l'IUT ne répond pas, sans autorisation de l'enseignant : 409
 * {@code ANNUAIRE_INDISPONIBLE}. Le front propose de relancer avec {@code autoriserIdentiteNonVerifiee}.
 */
public class AnnuaireIndisponibleException extends ExceptionConflit {

    public AnnuaireIndisponibleException() {
        super("L'annuaire de l'IUT est indisponible. Autoriser l'entrée en identité non vérifiée ?");
    }

    @Override
    public String code() {
        return "ANNUAIRE_INDISPONIBLE";
    }
}
