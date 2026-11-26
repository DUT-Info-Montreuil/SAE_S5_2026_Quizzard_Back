package fr.iutmontreuil.quizzard.features.seance.application;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.features.seance.application.dto.CorrectionQuestion;
import fr.iutmontreuil.quizzard.features.seance.application.dto.DeroulementSeance;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QrCode;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QuestionProjetee;
import fr.iutmontreuil.quizzard.features.seance.application.dto.RepartitionReponses;
import fr.iutmontreuil.quizzard.features.seance.application.port.GenerateurQrCode;
import fr.iutmontreuil.quizzard.features.seance.application.port.LiensAcces;
import fr.iutmontreuil.quizzard.features.seance.application.port.SuiviSeanceRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionDeSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.Quiz;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;

/** Construit l'état d'une séance pour l'écran E5, dans la transaction du cas d'usage appelant. */
@Component
class VueEnseignant {

    private final SuiviSeanceRepository suivi;
    private final LiensAcces liens;
    private final GenerateurQrCode generateurQrCode;
    private final Clock horloge;

    VueEnseignant(SuiviSeanceRepository suivi, LiensAcces liens, GenerateurQrCode generateurQrCode, Clock horloge) {
        this.suivi = suivi;
        this.liens = liens;
        this.generateurQrCode = generateurQrCode;
        this.horloge = horloge;
    }

    DeroulementSeance decrire(SeanceAnimee seance, Quiz quiz) {
        String lien = null;
        QrCode qrCode = null;
        if (!seance.estTerminee()) {
            lien = liens.lienAcces(seance.jeton());
            qrCode = generateurQrCode.generer(lien);
        }
        QuestionProjetee projetee = null;
        Long tempsRestantMs = null;
        RepartitionReponses reponses = null;
        CorrectionQuestion correction = null;
        Optional<QuestionDeSeance> courante = quiz.question(seance.positionCourante());
        if (courante.isPresent()) {
            QuestionDeSeance question = courante.get();
            int position = seance.positionCourante();
            projetee = QuestionProjetee.de(question, position, seance.echeanceQuestion());
            reponses = suivi.repartition(seance.idSession(), question.idQuestion());
            if (seance.aUneQuestionOuverte()) {
                tempsRestantMs = Math.max(0,
                        Duration.between(LocalDateTime.now(horloge), seance.echeanceQuestion()).toMillis());
            } else {
                correction = CorrectionQuestion.de(question, position, reponses);
            }
        }
        return new DeroulementSeance(seance, quiz.titre(), quiz.nbQuestions(), lien, qrCode,
                suivi.compterConnectes(seance.idSession()), projetee, tempsRestantMs, reponses, correction);
    }
}
