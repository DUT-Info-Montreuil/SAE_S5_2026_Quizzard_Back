package fr.iutmontreuil.quizzard.features.cours.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.iutmontreuil.quizzard.features.cours.application.dto.InsererPageQuizDto;
import fr.iutmontreuil.quizzard.features.cours.application.dto.PageQuizInseree;
import fr.iutmontreuil.quizzard.features.cours.application.port.CoursRepository;
import fr.iutmontreuil.quizzard.features.cours.application.port.PageInsereeRepository;
import fr.iutmontreuil.quizzard.features.cours.application.port.QuizRepository;
import fr.iutmontreuil.quizzard.features.cours.domain.Cours;
import fr.iutmontreuil.quizzard.features.cours.domain.CoursArchiveException;
import fr.iutmontreuil.quizzard.features.cours.domain.CoursIntrouvableException;
import fr.iutmontreuil.quizzard.features.cours.domain.EmplacementInvalideException;
import fr.iutmontreuil.quizzard.features.cours.domain.FichierVersion;
import fr.iutmontreuil.quizzard.features.cours.domain.PageInseree;
import fr.iutmontreuil.quizzard.features.cours.domain.PageInseree.TypePage;
import fr.iutmontreuil.quizzard.features.cours.domain.PagePlacee;
import fr.iutmontreuil.quizzard.features.cours.domain.PageQuiz;
import fr.iutmontreuil.quizzard.features.cours.domain.Question;
import fr.iutmontreuil.quizzard.features.cours.domain.Quiz;
import fr.iutmontreuil.quizzard.features.cours.domain.QuizDuCours;
import fr.iutmontreuil.quizzard.features.cours.domain.QuizEnSeanceException;
import fr.iutmontreuil.quizzard.features.cours.domain.QuizIntrouvableException;
import fr.iutmontreuil.quizzard.features.cours.domain.VersionCours;
import fr.iutmontreuil.quizzard.features.cours.domain.VersionPerimeeException;
import fr.iutmontreuil.quizzard.shared.application.port.JournalEvenements;

/** UC3 avec des doublures en mémoire : scénario nominal et chaque ligne métier du tableau des cas d'erreur. */
class InsererPageQuizUseCaseTest {

    private static final Long ENSEIGNANT = 7L;
    private static final Long COURS = 1L;
    private static final Long VERSION = 10L;
    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-24T14:00:00Z"), ZoneOffset.UTC);

    private final Map<Long, Cours> lesCours = new HashMap<>();
    private final Map<Long, Quiz> lesQuiz = new HashMap<>();
    private final Set<Long> quizEnSeance = new HashSet<>();
    private final List<PageQuiz> lesPages = new ArrayList<>();
    private final List<String> evenements = new ArrayList<>();
    private LocalDateTime coursModifieLe;
    private long prochainId = 100;
    private InsererPageQuizUseCase useCase;

    @BeforeEach
    void preparer() {
        lesCours.put(COURS, new Cours(COURS, ENSEIGNANT, false, new VersionCours(VERSION, 1, 12)));
        lesQuiz.put(50L, new Quiz(50L, COURS, "Quiz 1", false, 20, false, "PROJETE_SEUL", false));
        useCase = new InsererPageQuizUseCase(() -> ENSEIGNANT, new Cours_(), new Quiz_(), new Pages_(),
                new Journal_(), HORLOGE);
    }

    private PageQuizInseree inserer(int page, int ordre, Long idQuiz) {
        return useCase.executer(new InsererPageQuizDto(COURS, VERSION, page, ordre, idQuiz, " repère "));
    }

    @Test
    void sans_quiz_choisi_un_quiz_est_cree_avec_le_numero_suivant() {
        PageQuizInseree resultat = inserer(4, 0, null);

        assertThat(resultat.quizCree()).isTrue();
        assertThat(resultat.quiz().titre()).isEqualTo("Quiz 2");
        assertThat(resultat.idQuestion()).isNotNull();
        assertThat(resultat.page().idQuestion()).isEqualTo(resultat.idQuestion());
        assertThat(resultat.page().repereTexte()).isEqualTo("repère");
        assertThat(resultat.pages()).extracting(PageInseree::numeroPage).containsExactly(4);
        assertThat(coursModifieLe).isEqualTo(LocalDateTime.now(HORLOGE));
        assertThat(evenements).containsExactly("page_quiz_inseree");
    }

    @Test
    void avec_un_quiz_existant_la_question_lui_est_ajoutee() {
        PageQuizInseree resultat = inserer(4, 0, 50L);
        assertThat(resultat.quizCree()).isFalse();
        assertThat(resultat.quiz().idQuiz()).isEqualTo(50L);
    }

    @Test
    void inserer_au_meme_endroit_decale_les_pages_de_rang_superieur_ou_egal() {
        inserer(4, 0, 50L);
        inserer(4, 0, 50L);
        assertThat(lesPages).extracting(PageQuiz::ordre).containsExactlyInAnyOrder(0, 1);
        assertThat(lesPages.getFirst().ordre()).isEqualTo(1);
    }

    @Test
    void un_cours_d_un_autre_enseignant_est_introuvable() {
        lesCours.put(COURS, new Cours(COURS, 99L, false, new VersionCours(VERSION, 1, 12)));
        assertThatThrownBy(() -> inserer(4, 0, null)).isInstanceOf(CoursIntrouvableException.class);
    }

    @Test
    void un_cours_archive_refuse_l_insertion() {
        lesCours.put(COURS, new Cours(COURS, ENSEIGNANT, true, new VersionCours(VERSION, 1, 12)));
        assertThatThrownBy(() -> inserer(4, 0, null)).isInstanceOf(CoursArchiveException.class);
    }

    @Test
    void une_version_qui_n_est_plus_la_derniere_est_refusee() {
        lesCours.put(COURS, new Cours(COURS, ENSEIGNANT, false, new VersionCours(11L, 2, 12)));
        assertThatThrownBy(() -> inserer(4, 0, null)).isInstanceOf(VersionPerimeeException.class);
    }

    @Test
    void un_emplacement_hors_du_cours_est_refuse_sans_rien_creer() {
        assertThatThrownBy(() -> inserer(13, 0, null)).isInstanceOf(EmplacementInvalideException.class);
        assertThatThrownBy(() -> inserer(4, 1, null)).isInstanceOf(EmplacementInvalideException.class);
        assertThat(lesPages).isEmpty();
        assertThat(lesQuiz).hasSize(1);
    }

    @Test
    void un_quiz_d_un_autre_cours_ou_archive_est_introuvable() {
        lesQuiz.put(60L, new Quiz(60L, 2L, "Quiz 1", false, 20, false, "PROJETE_SEUL", false));
        assertThatThrownBy(() -> inserer(4, 0, 60L)).isInstanceOf(QuizIntrouvableException.class);
        assertThatThrownBy(() -> inserer(4, 0, 404L)).isInstanceOf(QuizIntrouvableException.class);
    }

    @Test
    void un_quiz_en_cours_de_seance_refuse_l_insertion() {
        quizEnSeance.add(50L);
        assertThatThrownBy(() -> inserer(4, 0, 50L)).isInstanceOf(QuizEnSeanceException.class);
        assertThat(lesPages).isEmpty();
    }

    // ---- Doublures ----

    private class Cours_ implements CoursRepository {
        @Override
        public Optional<Cours> trouverPourModification(Long idCours, Long idEnseignant) {
            return Optional.ofNullable(lesCours.get(idCours)).filter(c -> c.idEnseignant().equals(idEnseignant));
        }

        @Override
        public void marquerModifie(Long idCours, LocalDateTime maintenant) {
            coursModifieLe = maintenant;
        }

        @Override
        public Optional<Cours> trouver(Long idCours, Long idEnseignant) {
            return trouverPourModification(idCours, idEnseignant);
        }

        @Override
        public boolean versionAppartient(Long idVersion, Long idCours) {
            return VERSION.equals(idVersion);
        }

        @Override
        public Optional<FichierVersion> fichierDe(Long idVersion, Long idCours) {
            throw new UnsupportedOperationException();
        }
    }

    private class Quiz_ implements QuizRepository {
        @Override
        public Optional<Quiz> trouverActif(Long idQuiz, Long idCours) {
            return Optional.ofNullable(lesQuiz.get(idQuiz))
                    .filter(q -> q.idCours().equals(idCours) && !q.archive());
        }

        @Override
        public List<QuizDuCours> listerActifs(Long idCours) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int compterQuiz(Long idCours) {
            return (int) lesQuiz.values().stream().filter(q -> q.idCours().equals(idCours)).count();
        }

        @Override
        public boolean estEnSeance(Long idQuiz) {
            return quizEnSeance.contains(idQuiz);
        }

        @Override
        public Quiz enregistrer(Quiz quiz) {
            if (quiz.estNouveau()) {
                quiz.identifier(prochainId++);
                lesQuiz.put(quiz.idQuiz(), quiz);
            }
            quiz.nouvellesQuestions().forEach((Question q) -> q.identifier(prochainId++));
            return quiz;
        }
    }

    private class Pages_ implements PageInsereeRepository {
        @Override
        public int compterAuMemeEndroit(Long idVersion, int numeroPage) {
            return (int) lesPages.stream().filter(p -> p.numeroPage() == numeroPage).count();
        }

        @Override
        public void decalerOrdres(Long idVersion, int numeroPage, int aPartirDe) {
            List<PageQuiz> decalees = lesPages.stream()
                    .filter(p -> p.numeroPage() == numeroPage && p.ordre() >= aPartirDe)
                    .map(p -> {
                        PageQuiz copie = PageQuiz.creer(p.idVersion(), p.numeroPage(), p.ordre() + 1, p.repereTexte(),
                                p.idQuestion());
                        copie.identifier(p.idPage());
                        return copie;
                    }).toList();
            lesPages.removeIf(p -> p.numeroPage() == numeroPage && p.ordre() >= aPartirDe);
            lesPages.addAll(0, decalees);
        }

        @Override
        public PageQuiz enregistrer(PageQuiz page) {
            page.identifier(prochainId++);
            lesPages.add(page);
            return page;
        }

        @Override
        public List<PageInseree> listerParVersion(Long idVersion) {
            return lesPages.stream()
                    .map(p -> new PageInseree(p.idPage(), p.numeroPage(), p.ordre(), TypePage.QUIZ, p.idQuestion(),
                            null, null))
                    .toList();
        }

        // Retrait et déplacement : testés de bout en bout sur PostgreSQL (PagesWebTest), pas par l'insertion.

        @Override
        public Optional<PagePlacee> trouverParId(Long idPage) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int compterAuMemeEndroitSauf(Long idVersion, int numeroPage, Long idPageExclue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void decalerOrdresSauf(Long idVersion, int numeroPage, int aPartirDe, Long idPageExclue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void placer(Long idPage, int numeroPage, int ordre) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void supprimer(Long idPage) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void recaler(Long idVersion, int numeroPage, Long idPageExclue) {
            throw new UnsupportedOperationException();
        }
    }

    private class Journal_ implements JournalEvenements {
        @Override
        public void evenement(String nom, Map<String, Object> champs) {
            evenements.add(nom);
        }

        @Override
        public void regleRefusee(String nom, Map<String, Object> champs) {
        }

        @Override
        public void actionSensible(String nom, Map<String, Object> champs) {
        }
    }
}
