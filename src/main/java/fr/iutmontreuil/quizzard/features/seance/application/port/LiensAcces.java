package fr.iutmontreuil.quizzard.features.seance.application.port;

import fr.iutmontreuil.quizzard.features.seance.domain.JetonSession;

/** Adresse que les étudiants ouvrent pour rejoindre une séance. */
public interface LiensAcces {

    /** Lien de la page « Rejoindre » du front pour ce jeton. */
    String lienAcces(JetonSession jeton);
}
