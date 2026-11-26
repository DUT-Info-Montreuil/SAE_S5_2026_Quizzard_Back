package fr.iutmontreuil.quizzard.features.seance.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import fr.iutmontreuil.quizzard.features.seance.application.dto.CorrectionQuestion;
import fr.iutmontreuil.quizzard.features.seance.application.dto.EcranAAfficher;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QrCode;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QuestionProjetee;
import fr.iutmontreuil.quizzard.features.seance.application.dto.RepartitionReponses;
import fr.iutmontreuil.quizzard.features.seance.application.dto.ResultatPersonnel;
import fr.iutmontreuil.quizzard.features.seance.application.dto.SeanceOuverte;
import fr.iutmontreuil.quizzard.features.seance.application.port.AnnuaireLdap;
import fr.iutmontreuil.quizzard.features.seance.application.port.DiffuseurSeance;
import fr.iutmontreuil.quizzard.features.seance.application.port.GenerateurQrCode;
import fr.iutmontreuil.quizzard.features.seance.application.port.MinuterieSeance;
import fr.iutmontreuil.quizzard.features.seance.application.port.QuizDeSeanceRepository;
import fr.iutmontreuil.quizzard.features.seance.application.port.SeanceAnimeeRepository;
import fr.iutmontreuil.quizzard.features.seance.application.port.SuiviSeanceRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.EcranIntermediaire;
import fr.iutmontreuil.quizzard.features.seance.domain.JetonSession;
import fr.iutmontreuil.quizzard.features.seance.domain.Quiz;
import fr.iutmontreuil.quizzard.features.seance.domain.ScoreParticipant;
import fr.iutmontreuil.quizzard.features.seance.domain.SeanceAnimee;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/**
 * Doublures en mémoire des ports de l'UC7, et les cas d'usage assemblés avec elles. Les appels qui comptent pour
 * l'ordre et le tout-ou-rien sont notés dans {@link #appels}.
 */
class DoublesLancement {

    static final String URL_FRONT = "http://front.test";

    final List<String> appels = new ArrayList<>();
    final Horloge horloge = new Horloge();
    final Quizzes quizzes = new Quizzes();
    final Seances seances = new Seances();
    final Suivi suivi = new Suivi();
    final Annuaire annuaire = new Annuaire();
    final Diffuseur diffuseur = new Diffuseur();
    final Minuterie minuterie = new Minuterie();
    final Journal journal = new Journal();
    int jetons;
    RuntimeException panneQrCode;

    private final GenerateurQrCode qrCodes = lien -> {
        appels.add("qrCode");
        if (panneQrCode != null) {
            throw panneQrCode;
        }
        return new QrCode(lien.getBytes());
    };

    LancerQuizUseCase lancer() {
        return new LancerQuizUseCase(quizzes, seances, annuaire, () -> new JetonSession("jeton-" + ++jetons),
                qrCodes, jeton -> URL_FRONT + "/rejoindre/" + jeton.valeur(), diffuseur, journal, horloge);
    }

    private AccesSeance acces() {
        return new AccesSeance(seances, quizzes);
    }

    private VueEnseignant vue() {
        return new VueEnseignant(suivi, jeton -> URL_FRONT + "/rejoindre/" + jeton.valeur(), qrCodes, horloge);
    }

    private FinDeQuestion finDeQuestion() {
        return new FinDeQuestion(seances, suivi, diffuseur, minuterie, journal, horloge);
    }

    private FinDeSeance finDeSeance() {
        return new FinDeSeance(finDeQuestion(), seances, diffuseur, journal, horloge);
    }

    DemarrerQuestionUseCase demarrer() {
        return new DemarrerQuestionUseCase(acces(), finDeQuestion(), finDeSeance(), vue(), seances, diffuseur,
                minuterie, journal, horloge);
    }

    CloreQuestionUseCase clore() {
        return new CloreQuestionUseCase(acces(), finDeQuestion(), vue(), seances, diffuseur, minuterie, horloge);
    }

    CloturerSeanceUseCase cloturer() {
        return new CloturerSeanceUseCase(acces(), finDeSeance(), vue());
    }

    ConsulterSeanceUseCase consulter() {
        return new ConsulterSeanceUseCase(acces(), vue(), seances);
    }

    /** Horloge du serveur que le test avance. */
    static final class Horloge extends Clock {

        private Instant maintenant = Instant.parse("2026-11-25T14:00:00Z");

        void avancer(Duration duree) {
            maintenant = maintenant.plus(duree);
        }

        LocalDateTime maintenant() {
            return LocalDateTime.ofInstant(maintenant, ZoneOffset.UTC);
        }

        @Override
        public Instant instant() {
            return maintenant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    final class Quizzes implements QuizDeSeanceRepository {

        final Map<Long, Quiz> parId = new LinkedHashMap<>();
        final List<Long> verrouillees = new ArrayList<>();

        void ajouter(Quiz quiz) {
            parId.put(quiz.idQuiz(), quiz);
        }

        @Override
        public Optional<Quiz> trouverParId(Long idQuiz) {
            return Optional.ofNullable(parId.get(idQuiz));
        }

        @Override
        public Optional<Quiz> trouverPourSeance(Long idQuiz, Long idVersion) {
            return trouverParId(idQuiz);
        }

        @Override
        public void verrouillerQuestions(Collection<Long> idQuestions) {
            verrouillees.addAll(idQuestions);
        }
    }

    final class Seances implements SeanceAnimeeRepository {

        final Map<Long, SeanceAnimee> parId = new LinkedHashMap<>();
        SeanceOuverte ouverte;

        @Override
        public void verrouillerLancements(Long idEnseignant) {
            appels.add("verrou");
        }

        @Override
        public Optional<SeanceOuverte> trouverOuverteParEnseignant(Long idEnseignant) {
            appels.add("chercherOuverte");
            return Optional.ofNullable(ouverte);
        }

        @Override
        public SeanceAnimee enregistrer(SeanceAnimee seance) {
            appels.add("enregistrer");
            SeanceAnimee enregistree = seance.avecIdentifiant((long) parId.size() + 1);
            parId.put(enregistree.idSession(), enregistree);
            return enregistree;
        }

        @Override
        public Optional<SeanceAnimee> trouverParId(Long idSession) {
            return Optional.ofNullable(parId.get(idSession));
        }

        @Override
        public Optional<SeanceAnimee> trouverPourModification(Long idSession) {
            return trouverParId(idSession);
        }

        @Override
        public void mettreAJour(SeanceAnimee seance) {
            parId.put(seance.idSession(), seance);
        }

        @Override
        public List<QuestionOuverte> listerQuestionsOuvertes() {
            return parId.values().stream()
                    .filter(SeanceAnimee::aUneQuestionOuverte)
                    .map(s -> new QuestionOuverte(s.idSession(), s.positionCourante(), s.echeanceQuestion()))
                    .toList();
        }
    }

    static final class Suivi implements SuiviSeanceRepository {

        int connectes;
        RepartitionReponses repartition = new RepartitionReponses(0, Map.of());
        List<ScoreParticipant> scores = List.of();

        @Override
        public int compterConnectes(Long idSession) {
            return connectes;
        }

        @Override
        public RepartitionReponses repartition(Long idSession, Long idQuestion) {
            return repartition;
        }

        @Override
        public List<ScoreParticipant> scores(Long idSession, Long idQuestion) {
            return scores;
        }
    }

    final class Annuaire implements AnnuaireLdap {

        boolean disponible = true;

        @Override
        public boolean estDisponible() {
            appels.add("annuaire");
            return disponible;
        }
    }

    final class Diffuseur implements DiffuseurSeance {

        final List<QuestionProjetee> questions = new ArrayList<>();
        final List<CorrectionQuestion> corrections = new ArrayList<>();
        final List<ResultatPersonnel> resultats = new ArrayList<>();
        final List<EcranAAfficher> ecrans = new ArrayList<>();
        final List<Integer> fins = new ArrayList<>();
        RuntimeException panne;

        @Override
        public void ouvrirCanal(SeanceAnimee seance) {
            appels.add("ouvrirCanal");
            if (panne != null) {
                throw panne;
            }
        }

        @Override
        public void annoncerQuestion(String codeAcces, QuestionProjetee question) {
            appels.add("question");
            questions.add(question);
        }

        @Override
        public void annoncerCorrection(String codeAcces, CorrectionQuestion correction,
                                       List<ResultatPersonnel> envoyes) {
            appels.add("correction");
            corrections.add(correction);
            resultats.addAll(envoyes);
        }

        @Override
        public void afficherEcran(String codeAcces, EcranAAfficher ecran) {
            ecrans.add(ecran);
        }

        @Override
        public void annoncerFin(String codeAcces, int position) {
            appels.add("fin");
            fins.add(position);
        }
    }

    /** Note les tâches demandées sans les exécuter. */
    static final class Minuterie implements MinuterieSeance {

        record Tache(String type, Long idSession, int position, LocalDateTime moment) {
        }

        final List<Tache> taches = new ArrayList<>();

        @Override
        public void programmerCloture(Long idSession, int position, LocalDateTime echeance) {
            taches.add(new Tache("cloture", idSession, position, echeance));
        }

        @Override
        public void programmerEcran(Long idSession, int position, EcranIntermediaire ecran, LocalDateTime moment) {
            taches.add(new Tache("ecran-" + ecran.type(), idSession, position, moment));
        }

        @Override
        public void programmerQuestionSuivante(Long idSession, int position, LocalDateTime moment) {
            taches.add(new Tache("passage", idSession, position, moment));
        }
    }

    final class Journal implements JournalEvenements {

        record Ligne(String type, String nom, Map<String, Object> champs) {
        }

        final List<Ligne> lignes = new ArrayList<>();

        @Override
        public void evenement(String nom, Map<String, Object> champs) {
            lignes.add(new Ligne("evenement", nom, champs));
        }

        @Override
        public void regleRefusee(String nom, Map<String, Object> champs) {
            lignes.add(new Ligne("regleRefusee", nom, champs));
        }

        @Override
        public void actionSensible(String nom, Map<String, Object> champs) {
            appels.add("journal");
            lignes.add(new Ligne("actionSensible", nom, champs));
        }
    }
}
