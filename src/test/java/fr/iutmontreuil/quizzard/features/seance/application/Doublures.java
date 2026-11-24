package fr.iutmontreuil.quizzard.features.seance.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import fr.iutmontreuil.quizzard.features.seance.application.port.DiffuseurTempsReel;
import fr.iutmontreuil.quizzard.features.seance.application.port.ParticipantRepository;
import fr.iutmontreuil.quizzard.features.seance.application.port.QuestionRepository;
import fr.iutmontreuil.quizzard.features.seance.application.port.ReponseRepository;
import fr.iutmontreuil.quizzard.features.seance.application.port.SessionQuizRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.Bareme;
import fr.iutmontreuil.quizzard.features.seance.domain.EtatSession;
import fr.iutmontreuil.quizzard.features.seance.domain.JetonParticipant;
import fr.iutmontreuil.quizzard.features.seance.domain.Participant;
import fr.iutmontreuil.quizzard.features.seance.domain.Proposition;
import fr.iutmontreuil.quizzard.features.seance.domain.Question;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionQcm;
import fr.iutmontreuil.quizzard.features.seance.domain.Reponse;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionQuiz;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/** Doublures en mémoire des ports de la séance, écrites à la main (DESIGN.md §10, tests de la couche application). */
final class Doublures {

    static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 24, 14, 0, 0);
    static final Clock HORLOGE = Clock.fixed(MAINTENANT.atZone(PARIS).toInstant(), PARIS);

    static final String CODE = "ABC123";
    static final String JETON = "jeton-secret-de-test";
    static final Long ID_SESSION = 1L;
    static final Long ID_PARTICIPANT = 10L;
    static final Long ID_QUESTION = 12L;

    private Doublures() {
    }

    /** QCM de 20 s : 41 juste, 42 fausse ; 1000 points avec décroissance. */
    static QuestionQcm qcm() {
        return new QuestionQcm(ID_QUESTION, "Quel protocole sert à l'annuaire ?", 20, new Bareme(1000, true, false),
                List.of(new Proposition(41L, "LDAP", true, 0), new Proposition(42L, "SMTP", false, 1)));
    }

    /** Séance en cours, question 12 ouverte depuis 5 s (échéance dans 15 s). */
    static SessionQuiz seanceEnCours() {
        return new SessionQuiz(ID_SESSION, CODE, EtatSession.EN_COURS, false, 1, ID_QUESTION,
                MAINTENANT.plusSeconds(15));
    }

    static class Sessions implements SessionQuizRepository {
        final Map<String, SessionQuiz> parCode = new HashMap<>();

        @Override
        public Optional<SessionQuiz> trouverParCodeAcces(String codeAcces) {
            return Optional.ofNullable(parCode.get(codeAcces));
        }
    }

    static class Participants implements ParticipantRepository {
        final Map<String, Participant> parEmpreinte = new HashMap<>();
        final Map<Long, Integer> pointsAjoutes = new HashMap<>();

        void ajouter(String jeton, Participant participant) {
            parEmpreinte.put(JetonParticipant.empreinte(jeton), participant);
        }

        @Override
        public Optional<Participant> trouverParEmpreinteJeton(String empreinte) {
            return Optional.ofNullable(parEmpreinte.get(empreinte));
        }

        @Override
        public void ajouterPoints(Long idParticipant, int points, LocalDateTime maintenant) {
            pointsAjoutes.merge(idParticipant, points, Integer::sum);
        }
    }

    static class Questions implements QuestionRepository {
        final Map<Long, Question> parId = new HashMap<>();

        @Override
        public Optional<Question> trouverParId(Long idQuestion) {
            return Optional.ofNullable(parId.get(idQuestion));
        }
    }

    static class Reponses implements ReponseRepository {
        final List<Reponse> enregistrees = new ArrayList<>();

        @Override
        public boolean existePour(Long idParticipant, Long idQuestion) {
            return enregistrees.stream().anyMatch(r -> r.idParticipant().equals(idParticipant)
                    && r.idQuestion().equals(idQuestion));
        }

        @Override
        public Reponse enregistrer(Reponse reponse) {
            enregistrees.add(reponse);
            reponse.identifier((long) enregistrees.size());
            return reponse;
        }
    }

    static class Diffuseur implements DiffuseurTempsReel {
        final List<String> signales = new ArrayList<>();

        @Override
        public void signalerNouvelleReponse(String codeAcces, Long idQuestion) {
            signales.add(codeAcces + "/" + idQuestion);
        }
    }

    static class Journal implements JournalEvenements {
        final List<String> refus = new ArrayList<>();
        final List<String> evenements = new ArrayList<>();

        @Override
        public void evenement(String nom, Map<String, Object> champs) {
            evenements.add(nom);
        }

        @Override
        public void regleRefusee(String nom, Map<String, Object> champs) {
            refus.add(nom);
        }

        @Override
        public void actionSensible(String nom, Map<String, Object> champs) {
            evenements.add(nom);
        }
    }
}
