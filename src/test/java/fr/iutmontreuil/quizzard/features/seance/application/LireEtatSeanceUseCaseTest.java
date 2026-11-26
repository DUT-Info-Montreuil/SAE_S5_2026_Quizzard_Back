package fr.iutmontreuil.quizzard.features.seance.application;

import static fr.iutmontreuil.quizzard.features.seance.application.Doublures.CODE;
import static fr.iutmontreuil.quizzard.features.seance.application.Doublures.ID_PARTICIPANT;
import static fr.iutmontreuil.quizzard.features.seance.application.Doublures.ID_QUESTION;
import static fr.iutmontreuil.quizzard.features.seance.application.Doublures.ID_SESSION;
import static fr.iutmontreuil.quizzard.features.seance.application.Doublures.JETON;
import static fr.iutmontreuil.quizzard.features.seance.application.Doublures.MAINTENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.iutmontreuil.quizzard.features.seance.application.dto.EtatSeanceEtudiant;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QuestionPourEtudiant;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QuestionPourEtudiant.Choix;
import fr.iutmontreuil.quizzard.features.seance.domain.Bareme;
import fr.iutmontreuil.quizzard.features.seance.domain.EtatSession;
import fr.iutmontreuil.quizzard.features.seance.domain.Paire;
import fr.iutmontreuil.quizzard.features.seance.domain.Participant;
import fr.iutmontreuil.quizzard.features.seance.domain.ParticipantInvalideException;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionPointsARelier;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionQuiz;
import fr.iutmontreuil.quizzard.features.seance.domain.TypeQuestion;

/** État de la séance vu par l'étudiant : question ouverte sans réponses attendues, déjà répondu, score. */
class LireEtatSeanceUseCaseTest {

    private final Doublures.Sessions sessions = new Doublures.Sessions();
    private final Doublures.Participants participants = new Doublures.Participants();
    private final Doublures.Questions questions = new Doublures.Questions();
    private final Doublures.Reponses reponses = new Doublures.Reponses();
    private LireEtatSeanceUseCase useCase;

    @BeforeEach
    void preparer() {
        sessions.parCode.put(CODE, Doublures.seanceEnCours());
        participants.ajouter(JETON, new Participant(ID_PARTICIPANT, ID_SESSION, 1250));
        questions.parId.put(ID_QUESTION, Doublures.qcm());
        IdentificationParticipant identification = new IdentificationParticipant(sessions, participants);
        useCase = new LireEtatSeanceUseCase(identification, questions, reponses);
    }

    @Test
    void la_question_ouverte_est_renvoyee_sans_les_reponses_attendues() {
        EtatSeanceEtudiant etat = useCase.executer(CODE, JETON);

        QuestionPourEtudiant question = etat.question();
        assertThat(question.idQuestion()).isEqualTo(ID_QUESTION);
        assertThat(question.type()).isEqualTo(TypeQuestion.QCM);
        assertThat(question.position()).isEqualTo(1);
        assertThat(question.echeance()).isEqualTo(MAINTENANT.plusSeconds(15));
        assertThat(question.propositions()).containsExactly(new Choix(41L, "LDAP"), new Choix(42L, "SMTP"));
        assertThat(question.choixMultiple()).isFalse();
        assertThat(etat.dejaRepondu()).isFalse();
        assertThat(etat.scoreTotal()).isEqualTo(1250);
    }

    @Test
    void sans_question_ouverte_la_question_est_nulle() {
        sessions.parCode.put(CODE, new SessionQuiz(ID_SESSION, CODE, EtatSession.PREPAREE, false, 0, null, null));
        EtatSeanceEtudiant etat = useCase.executer(CODE, JETON);
        assertThat(etat.question()).isNull();
        assertThat(etat.dejaRepondu()).isFalse();
    }

    @Test
    void les_colonnes_a_relier_sont_melangees_de_la_meme_facon_a_chaque_lecture() {
        questions.parId.put(ID_QUESTION, new QuestionPointsARelier(ID_QUESTION, "Relie", 20,
                new Bareme(1000, true, true), List.of(new Paire(61L, "80", "HTTP"), new Paire(62L, "53", "DNS"),
                new Paire(63L, "25", "SMTP"), new Paire(64L, "22", "SSH"))));

        QuestionPourEtudiant premiere = useCase.executer(CODE, JETON).question();
        QuestionPourEtudiant seconde = useCase.executer(CODE, JETON).question();

        assertThat(premiere.elementsGauche()).extracting(Choix::libelle)
                .containsExactlyInAnyOrder("80", "53", "25", "22");
        assertThat(premiere.elementsDroite()).extracting(Choix::libelle)
                .containsExactlyInAnyOrder("HTTP", "DNS", "SMTP", "SSH");
        assertThat(seconde).isEqualTo(premiere);
        assertThat(premiere.propositions()).isEmpty();
    }

    @Test
    void un_jeton_inconnu_est_refuse() {
        assertThatThrownBy(() -> useCase.executer(CODE, "inconnu")).isInstanceOf(ParticipantInvalideException.class);
    }
}
