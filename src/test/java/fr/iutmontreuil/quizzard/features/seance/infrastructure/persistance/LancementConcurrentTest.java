package fr.iutmontreuil.quizzard.features.seance.infrastructure.persistance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;
import fr.iutmontreuil.quizzard.features.seance.FixturesLancement;
import fr.iutmontreuil.quizzard.features.seance.FixturesLancement.Enseignant;
import fr.iutmontreuil.quizzard.features.seance.FixturesLancement.QuizDeTest;
import fr.iutmontreuil.quizzard.features.seance.application.LancerQuizUseCase;
import fr.iutmontreuil.quizzard.features.seance.application.dto.LancerQuizDto;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionDejaOuverteException;

/** Deux onglets qui lancent au même instant : une seule séance ouverte par enseignant. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LancementConcurrentTest {

    @Autowired
    LancerQuizUseCase lancerQuiz;
    @Autowired
    JdbcClient jdbc;

    FixturesLancement fixtures;

    @BeforeEach
    void preparer() {
        fixtures = new FixturesLancement(jdbc);
    }

    @AfterEach
    void nettoyer() {
        fixtures.nettoyer();
    }

    @RepeatedTest(3)
    void de_deux_lancements_simultanes_un_seul_reussit() throws Exception {
        Enseignant enseignant = fixtures.creerEnseignant();
        List<QuizDeTest> quizzes = List.of(fixtures.creerQuiz(enseignant, false),
                fixtures.creerQuiz(enseignant, false));
        quizzes.forEach(q -> fixtures.ajouterQcm(q, 2, "Quel protocole ?", null));
        CountDownLatch depart = new CountDownLatch(1);
        List<Throwable> refus = new ArrayList<>();

        try (ExecutorService fils = Executors.newFixedThreadPool(quizzes.size())) {
            List<Future<?>> lancements = new ArrayList<>();
            for (QuizDeTest quiz : quizzes) {
                lancements.add(fils.submit(() -> {
                    depart.await();
                    return lancerQuiz.executer(new LancerQuizDto(quiz.idQuiz(), enseignant.id(), false));
                }));
            }
            depart.countDown();
            for (Future<?> lancement : lancements) {
                try {
                    lancement.get();
                } catch (ExecutionException e) {
                    refus.add(e.getCause());
                }
            }
        }

        assertThat(refus).singleElement().isInstanceOf(SessionDejaOuverteException.class);
        assertThat(jdbc.sql("""
                        SELECT count(*) FROM session_quiz s JOIN quiz q ON q.id_quiz = s.id_quiz
                          JOIN cours c ON c.id_cours = q.id_cours WHERE c.id_enseignant = :id
                        """)
                .param("id", enseignant.id()).query(Integer.class).single()).isEqualTo(1);
    }
}
