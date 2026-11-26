package fr.iutmontreuil.quizzard.features.seance.application.port;

import fr.iutmontreuil.quizzard.features.seance.application.GenerationAccesException;
import fr.iutmontreuil.quizzard.features.seance.domain.JetonSession;

/** Génération du jeton d'accès à une séance ; le dossier de conception nomme ce port {@code GenerateurJeton}. */
public interface GenerateurJetonSession {

    /**
     * Jeton aléatoire non devinable, qui sert à construire le lien d'accès des étudiants.
     *
     * @throws GenerationAccesException si le tirage aléatoire échoue
     */
    JetonSession generer();
}
