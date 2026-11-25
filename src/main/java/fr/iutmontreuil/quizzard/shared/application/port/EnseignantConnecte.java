package fr.iutmontreuil.quizzard.shared.application.port;

import fr.iutmontreuil.quizzard.shared.domain.ExceptionNonAuthentifie;

/**
 * Enseignant de la session de connexion. Les cas d'usage le lisent ici, jamais dans la requête, pour vérifier que
 * la ressource lui appartient (CLAUDE.md, règle 7).
 */
public interface EnseignantConnecte {

    /**
     * Identifiant de l'enseignant connecté.
     *
     * @throws ExceptionNonAuthentifie s'il n'y a pas d'enseignant connecté (401)
     */
    Long idEnseignant();
}
