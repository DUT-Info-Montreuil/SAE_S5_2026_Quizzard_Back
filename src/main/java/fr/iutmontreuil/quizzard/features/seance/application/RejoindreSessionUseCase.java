package fr.iutmontreuil.quizzard.features.seance.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.seance.application.dto.ParticipantRejoint;
import fr.iutmontreuil.quizzard.features.seance.application.dto.RejoindreSessionDto;
import fr.iutmontreuil.quizzard.features.seance.application.port.DiffuseurConnectes;
import fr.iutmontreuil.quizzard.features.seance.application.port.GenerateurJeton;
import fr.iutmontreuil.quizzard.features.seance.application.port.EntreeParticipantRepository;
import fr.iutmontreuil.quizzard.features.seance.application.port.SessionQuizRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.ConditionsEntree;
import fr.iutmontreuil.quizzard.features.seance.domain.ConnexionIutRequiseException;
import fr.iutmontreuil.quizzard.features.seance.domain.JetonParticipant;
import fr.iutmontreuil.quizzard.features.seance.domain.ParticipantEntrant;
import fr.iutmontreuil.quizzard.features.seance.domain.PseudonymeDejaPrisException;
import fr.iutmontreuil.quizzard.features.seance.domain.Pseudonyme;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionCloseException;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionIntrouvableException;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionQuiz;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/**
 * Cas d'utilisation UC8 « Rejoindre un quiz » : un étudiant entre dans une séance avec un simple pseudonyme.
 * <p>
 * L'entrée est tout ou rien : le participant est enregistré dans une seule transaction, et l'écriture est forcée
 * par le dépôt pour qu'une erreur de base soit détectée ici. Le nombre de connectés n'est envoyé à l'écran de
 * l'enseignant qu'une fois la transaction validée (c'est le diffuseur qui attend le commit).
 * <p>
 * Journal : l'entrée réussie ({@code participant_entre}) et les entrées refusées ({@code entree_refusee_…}) sont
 * tracées dans les logs seulement, avec l'identifiant de séance : jamais de pseudonyme, de jeton ni de code d'accès.
 * Ce ne sont pas des actions sensibles.
 */
@Service
public class RejoindreSessionUseCase {

    private final SessionQuizRepository sessions;
    private final EntreeParticipantRepository participants;
    private final GenerateurJeton generateurJeton;
    private final DiffuseurConnectes diffuseur;
    private final JournalEvenements journal;
    private final Clock horloge;

    public RejoindreSessionUseCase(SessionQuizRepository sessions, EntreeParticipantRepository participants,
                                   GenerateurJeton generateurJeton, DiffuseurConnectes diffuseur,
                                   JournalEvenements journal, Clock horloge) {
        this.sessions = sessions;
        this.participants = participants;
        this.generateurJeton = generateurJeton;
        this.diffuseur = diffuseur;
        this.journal = journal;
        this.horloge = horloge;
    }

    /**
     * @throws SessionIntrouvableException     le code d'accès ne correspond à aucune séance (404)
     * @throws SessionCloseException           la séance est close (409)
     * @throws ConnexionIutRequiseException    le quiz est noté (409)
     * @throws fr.iutmontreuil.quizzard.features.seance.domain.PseudonymeInvalideException pseudonyme invalide (422)
     * @throws PseudonymeDejaPrisException     pseudonyme déjà pris dans la séance, avec une proposition (409)
     */
    @Transactional
    public ParticipantRejoint executer(RejoindreSessionDto dto) {
        SessionQuiz session = sessions.trouverParCodeAcces(dto.codeAcces())
                .orElseThrow(SessionIntrouvableException::new);
        Long idSession = session.idSession();

        verifierEntree(session);
        Pseudonyme pseudonyme = Pseudonyme.de(dto.pseudonyme());
        if (participants.existePseudonyme(idSession, pseudonyme)) {
            throw refuserPseudonymePris(idSession, pseudonyme);
        }

        String jeton = generateurJeton.generer();
        ParticipantEntrant nouveau = ParticipantEntrant.rejoindre(idSession, pseudonyme,
                JetonParticipant.empreinte(jeton),
                LocalDateTime.now(horloge));
        ParticipantEntrant enregistre = participants.enregistrer(nouveau);

        journal.evenement("participant_entre", Map.of(
                "idSession", idSession,
                "idParticipant", enregistre.idParticipant()));
        diffuseur.signalerConnectes(session.codeAcces(), participants.compter(idSession));

        return new ParticipantRejoint(enregistre.idParticipant(), jeton, enregistre.pseudonyme().valeur());
    }

    /** Vérifie que la séance accepte une entrée sans compte, et trace le refus sans donnée personnelle. */
    private void verifierEntree(SessionQuiz session) {
        try {
            ConditionsEntree.verifierEntreeSansCompte(session);
        } catch (SessionCloseException e) {
            journal.regleRefusee("entree_refusee_session_close", Map.of("idSession", session.idSession()));
            throw e;
        } catch (ConnexionIutRequiseException e) {
            journal.regleRefusee("entree_refusee_quiz_note", Map.of("idSession", session.idSession()));
            throw e;
        }
    }

    /** Pseudonyme pris : trace le refus et prépare l'exception qui porte un pseudonyme libre à proposer. */
    private PseudonymeDejaPrisException refuserPseudonymePris(Long idSession, Pseudonyme pseudonyme) {
        Set<String> pris = participants.pseudonymesDeLaSession(idSession);
        journal.regleRefusee("entree_refusee_pseudonyme_pris", Map.of("idSession", idSession));
        return new PseudonymeDejaPrisException(pseudonyme.proposerAlternative(pris));
    }
}
