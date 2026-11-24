package fr.iutmontreuil.quizzard.features.seance.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.seance.application.IdentificationParticipant.ParticipantEnSeance;
import fr.iutmontreuil.quizzard.features.seance.application.dto.ReponseEnregistree;
import fr.iutmontreuil.quizzard.features.seance.application.dto.RepondreQuestionDto;
import fr.iutmontreuil.quizzard.features.seance.application.port.DiffuseurTempsReel;
import fr.iutmontreuil.quizzard.features.seance.application.port.ParticipantRepository;
import fr.iutmontreuil.quizzard.features.seance.application.port.QuestionRepository;
import fr.iutmontreuil.quizzard.features.seance.application.port.ReponseRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.ContenuReponse;
import fr.iutmontreuil.quizzard.features.seance.domain.DejaReponduException;
import fr.iutmontreuil.quizzard.features.seance.domain.HorsDelaiException;
import fr.iutmontreuil.quizzard.features.seance.domain.Question;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionCloseException;
import fr.iutmontreuil.quizzard.features.seance.domain.Reponse;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionQuiz;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/**
 * UC9 « Répondre à une question » : vérifie que la question est ouverte et le délai respecté, refuse une seconde
 * réponse, crée et valide la réponse, calcule ses points, l'enregistre et ajoute les points au score, dans une
 * seule transaction. Une erreur de base annule tout (rien n'est enregistré).
 */
@Service
public class RepondreQuestionUseCase {

    private final IdentificationParticipant identification;
    private final ParticipantRepository participants;
    private final QuestionRepository questions;
    private final ReponseRepository reponses;
    private final DiffuseurTempsReel diffuseur;
    private final JournalEvenements journal;
    private final Clock horloge;

    public RepondreQuestionUseCase(IdentificationParticipant identification, ParticipantRepository participants,
                                   QuestionRepository questions, ReponseRepository reponses,
                                   DiffuseurTempsReel diffuseur, JournalEvenements journal, Clock horloge) {
        this.identification = identification;
        this.participants = participants;
        this.questions = questions;
        this.reponses = reponses;
        this.diffuseur = diffuseur;
        this.journal = journal;
        this.horloge = horloge;
    }

    @Transactional
    public ReponseEnregistree executer(RepondreQuestionDto dto) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        ParticipantEnSeance acces = identification.identifier(dto.codeAcces(), dto.jeton());
        SessionQuiz session = acces.session();
        Long idParticipant = acces.participant().idParticipant();
        Map<String, Object> champs = Map.of("idSession", session.idSession(), "idQuestion",
                String.valueOf(dto.idQuestion()));

        if (!session.aUneQuestionOuverte()) {
            throw new QuestionCloseException();
        }
        Question question = questions.trouverParId(session.idQuestionOuverte())
                .orElseThrow(QuestionCloseException::new);
        long tempsMs;
        try {
            tempsMs = session.verifierReponseAcceptee(dto.idQuestion(), question.dureeSec(), maintenant);
        } catch (HorsDelaiException e) {
            journal.regleRefusee("reponse_hors_delai", champs);
            throw e;
        }
        if (reponses.existePour(idParticipant, question.idQuestion())) {
            journal.regleRefusee("reponse_deja_enregistree", champs);
            throw new DejaReponduException();
        }

        ContenuReponse contenu = new ContenuReponse(dto.idPropositions(), dto.liens(), dto.texteLibre());
        Reponse reponse = Reponse.creer(question, contenu, idParticipant, maintenant, tempsMs);
        reponse.valider(question);
        int points = question.evaluer(reponse, tempsMs);
        reponse.attribuerPoints(points, question.statutCorrection(reponse));

        reponses.enregistrer(reponse);
        participants.ajouterPoints(idParticipant, points, maintenant);
        diffuseur.signalerNouvelleReponse(session.codeAcces(), question.idQuestion());
        journal.evenement("reponse_enregistree", champs);
        return new ReponseEnregistree(reponse.idReponse(), question.idQuestion(), maintenant);
    }
}
