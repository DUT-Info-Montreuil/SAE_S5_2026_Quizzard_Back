package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import java.util.Base64;

import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.features.seance.application.dto.DeroulementSeance;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QrCode;
import fr.iutmontreuil.quizzard.features.seance.application.dto.SessionLancee;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;
import fr.iutmontreuil.quizzard.features.seance.infrastructure.format.FormatSeance;

/** Conversion des résultats des cas d'usage de l'UC7 en JSON pour le navigateur de l'enseignant. */
@Component
class SessionMapper {

    private final FormatSeance format;

    SessionMapper(FormatSeance format) {
        this.format = format;
    }

    SessionResponse versReponse(SessionLancee lancee) {
        SeanceAnimee seance = lancee.seance();
        return new SessionResponse(seance.idSession(), seance.idQuiz(), lancee.titre(), seance.etat().name(),
                seance.modeNote(), lancee.nbQuestions(), seance.codeAcces(), lancee.lienAcces(),
                base64(lancee.qrCode()), format.date(seance.dateLancement()));
    }

    DeroulementResponse versReponse(DeroulementSeance etat) {
        SeanceAnimee seance = etat.seance();
        return new DeroulementResponse(seance.idSession(), seance.idQuiz(), etat.titre(), seance.etat().name(),
                seance.modeNote(), etat.nbQuestions(), seance.positionCourante(), seance.codeAcces(),
                etat.lienAcces(), base64(etat.qrCode()), format.date(seance.dateLancement()),
                format.date(seance.dateCloture()), etat.connectes(), seance.aUneQuestionOuverte(),
                format.question(etat.question()), etat.tempsRestantMs(),
                etat.reponses() == null ? null : format.repartition(etat.question().idQuestion(), etat.reponses()),
                format.correction(etat.correction()));
    }

    private static String base64(QrCode qrCode) {
        return qrCode == null ? null : Base64.getEncoder().encodeToString(qrCode.png());
    }
}
