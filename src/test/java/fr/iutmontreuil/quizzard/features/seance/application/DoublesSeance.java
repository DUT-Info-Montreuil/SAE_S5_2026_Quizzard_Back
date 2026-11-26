package fr.iutmontreuil.quizzard.features.seance.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import fr.iutmontreuil.quizzard.features.seance.application.dto.TitresQuiz;
import fr.iutmontreuil.quizzard.features.seance.application.port.DiffuseurConnectes;
import fr.iutmontreuil.quizzard.features.seance.application.port.GenerateurJeton;
import fr.iutmontreuil.quizzard.features.seance.application.port.InformationsQuiz;
import fr.iutmontreuil.quizzard.features.seance.application.port.EntreeParticipantRepository;
import fr.iutmontreuil.quizzard.features.seance.application.port.SessionQuizRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.JetonParticipant;
import fr.iutmontreuil.quizzard.features.seance.domain.ParticipantEntrant;
import fr.iutmontreuil.quizzard.features.seance.domain.PseudonymeDejaPrisException;
import fr.iutmontreuil.quizzard.features.seance.domain.Pseudonyme;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionQuiz;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/** Doublures écrites à la main pour tester les cas d'usage de la feature {@code seance} sans Spring ni base. */
final class DoublesSeance {

    private DoublesSeance() {
    }

    /** Séances gardées en mémoire, retrouvées par code d'accès. */
    static class SessionsEnMemoire implements SessionQuizRepository {

        private final Map<String, SessionQuiz> parCode = new LinkedHashMap<>();

        SessionsEnMemoire avec(SessionQuiz session) {
            parCode.put(session.codeAcces(), session);
            return this;
        }

        @Override
        public Optional<SessionQuiz> trouverParCodeAcces(String codeAcces) {
            return Optional.ofNullable(parCode.get(codeAcces));
        }

        @Override
        public Optional<SessionQuiz> trouverParId(Long idSession) {
            return parCode.values().stream().filter(s -> s.idSession().equals(idSession)).findFirst();
        }
    }

    /** Participants gardés en mémoire ; peut simuler le refus de la contrainte d'unicité de la base. */
    static class ParticipantsEnMemoire implements EntreeParticipantRepository {

        final List<ParticipantEntrant> enregistres = new ArrayList<>();
        private boolean contrainteUniciteViolee;
        private long prochainId = 1;

        ParticipantsEnMemoire avecPseudonyme(Long idSession, String pseudonyme) {
            enregistres.add(ParticipantEntrant.rejoindre(idSession, Pseudonyme.de(pseudonyme), "b".repeat(64), null)
                    .avecIdentifiant(prochainId++));
            return this;
        }

        /** Le prochain {@link #enregistrer} échoue comme si un autre étudiant avait pris le pseudonyme entre-temps. */
        ParticipantsEnMemoire quiRefuseCommeUnDoublonSimultane() {
            contrainteUniciteViolee = true;
            return this;
        }

        @Override
        public boolean existePseudonyme(Long idSession, Pseudonyme pseudonyme) {
            return enregistres.stream().anyMatch(p -> p.idSession().equals(idSession)
                    && p.pseudonyme().equals(pseudonyme));
        }

        @Override
        public Set<String> pseudonymesDeLaSession(Long idSession) {
            Set<String> pseudonymes = new LinkedHashSet<>();
            enregistres.stream().filter(p -> p.idSession().equals(idSession))
                    .forEach(p -> pseudonymes.add(p.pseudonyme().valeur()));
            return pseudonymes;
        }

        @Override
        public ParticipantEntrant enregistrer(ParticipantEntrant participant) {
            if (contrainteUniciteViolee) {
                throw new PseudonymeDejaPrisException();
            }
            ParticipantEntrant enregistre = participant.avecIdentifiant(prochainId++);
            enregistres.add(enregistre);
            return enregistre;
        }

        @Override
        public int compter(Long idSession) {
            return (int) enregistres.stream().filter(p -> p.idSession().equals(idSession)).count();
        }
    }

    /** Remet toujours le même jeton ; {@code EMPREINTE} est la valeur attendue en base. */
    static class GenerateurFixe implements GenerateurJeton {

        static final String VALEUR = "jeton-secret-en-clair";
        static final String EMPREINTE = JetonParticipant.empreinte(VALEUR);

        @Override
        public String generer() {
            return VALEUR;
        }
    }

    /** Garde les messages envoyés à l'écran de l'enseignant. */
    static class DiffuseurEnregistreur implements DiffuseurConnectes {

        record Envoi(String codeAcces, int nombre) {
        }

        final List<Envoi> envois = new ArrayList<>();

        @Override
        public void signalerConnectes(String codeAcces, int nombre) {
            envois.add(new Envoi(codeAcces, nombre));
        }
    }

    /** Garde les événements et les refus écrits dans le journal. */
    static class JournalEnregistreur implements JournalEvenements {

        record Ligne(String nom, Map<String, Object> champs) {
        }

        final List<Ligne> evenements = new ArrayList<>();
        final List<Ligne> refus = new ArrayList<>();
        final List<Ligne> actionsSensibles = new ArrayList<>();

        @Override
        public void evenement(String nom, Map<String, Object> champs) {
            evenements.add(new Ligne(nom, champs));
        }

        @Override
        public void regleRefusee(String nom, Map<String, Object> champs) {
            refus.add(new Ligne(nom, champs));
        }

        @Override
        public void actionSensible(String nom, Map<String, Object> champs) {
            actionsSensibles.add(new Ligne(nom, champs));
        }
    }

    /** Titres fixes, quelle que soit la séance demandée. */
    static class InformationsFixes implements InformationsQuiz {

        @Override
        public TitresQuiz titresDeLaSession(Long idSession) {
            return new TitresQuiz("Réseaux", "Quiz du chapitre 3");
        }
    }
}
