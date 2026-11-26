package fr.iutmontreuil.quizzard.features.seance.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.seance.application.dto.SessionPublique;
import fr.iutmontreuil.quizzard.features.seance.application.dto.TitresQuiz;
import fr.iutmontreuil.quizzard.features.seance.application.port.InformationsQuiz;
import fr.iutmontreuil.quizzard.features.seance.application.port.SessionQuizRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.ConditionsEntree;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionCloseException;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionIntrouvableException;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionQuiz;

/**
 * Cas d'utilisation UC8, première étape : l'étudiant ouvre le lien ou scanne le QR code, le front lit ce qu'il faut
 * pour afficher l'écran E7 (titres, mode noté, état). Ne crée rien.
 */
@Service
public class ConsulterSessionPubliqueUseCase {

    private final SessionQuizRepository sessions;
    private final InformationsQuiz informationsQuiz;

    public ConsulterSessionPubliqueUseCase(SessionQuizRepository sessions, InformationsQuiz informationsQuiz) {
        this.sessions = sessions;
        this.informationsQuiz = informationsQuiz;
    }

    /**
     * @throws SessionIntrouvableException le code d'accès ne correspond à aucune séance (404)
     * @throws SessionCloseException       la séance est close (409)
     */
    @Transactional(readOnly = true)
    public SessionPublique executer(String codeAcces) {
        SessionQuiz session = sessions.trouverParCodeAcces(codeAcces).orElseThrow(SessionIntrouvableException::new);
        ConditionsEntree.verifierSeanceOuverte(session);
        TitresQuiz titres = informationsQuiz.titresDeLaSession(session.idSession());
        return new SessionPublique(titres.titreCours(), titres.titreQuiz(), session.estNotee(), session.etat());
    }
}
