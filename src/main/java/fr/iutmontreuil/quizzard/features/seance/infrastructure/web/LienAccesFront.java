package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.features.seance.application.port.LiensAcces;
import fr.iutmontreuil.quizzard.features.seance.domain.JetonSession;

/** Lien vers l'écran « Rejoindre » du front : {@code <FRONT_URL>/rejoindre/<codeAcces>}. */
@Component
class LienAccesFront implements LiensAcces {

    private final String urlFront;

    LienAccesFront(@Value("${quizzard.front.url}") String urlFront) {
        this.urlFront = urlFront.endsWith("/") ? urlFront.substring(0, urlFront.length() - 1) : urlFront;
    }

    @Override
    public String lienAcces(JetonSession jeton) {
        return urlFront + "/rejoindre/" + jeton.valeur();
    }
}
