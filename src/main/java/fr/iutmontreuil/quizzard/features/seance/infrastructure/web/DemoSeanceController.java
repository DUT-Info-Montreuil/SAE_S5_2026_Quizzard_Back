package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.iutmontreuil.quizzard.features.seance.application.LireEtatSeanceUseCase;
import fr.iutmontreuil.quizzard.features.seance.application.port.QuestionRepository;
import fr.iutmontreuil.quizzard.features.seance.application.port.SessionQuizRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.Question;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionIntrouvableException;
import fr.iutmontreuil.quizzard.features.seance.domain.SessionQuiz;
import fr.iutmontreuil.quizzard.features.seance.infrastructure.web.EtatSeanceResponse.QuestionResponse;
import javax.sql.DataSource;

/**
 * <b>Démonstration seulement</b> (profil Spring {@code demo}) : remplace « Lancer un quiz » (UC7) et « Rejoindre
 * un quiz » (UC8), pas encore codés, pour essayer l'UC9 avec le front. Au démarrage, crée la séance {@code DEMO}
 * (données de {@code demo/seance-demo.sql}, jeton {@code jeton-demo}) et ouvre sa première question. Les routes
 * ci-dessous font avancer la séance comme le fera l'UC7 et diffusent les mêmes messages STOMP.
 * <p>
 * À lancer avec {@code SECURITE_OUVERTE=true} : ces routes n'ont ni connexion ni CSRF. Ce bean n'existe pas hors
 * du profil {@code demo}. À supprimer quand l'UC7 et l'UC8 seront faits.
 */
@Profile("demo")
@RestController
@RequestMapping("/api/demo/sessions/{codeAcces}")
class DemoSeanceController implements ApplicationRunner {

    static final String CODE_DEMO = "DEMO";
    private static final Logger journal = LoggerFactory.getLogger(DemoSeanceController.class);

    private final DataSource source;
    private final JdbcClient jdbc;
    private final SessionQuizRepository sessions;
    private final QuestionRepository questions;
    private final SimpMessagingTemplate messages;
    private final SeanceMapper mapper;
    private final Clock horloge;

    DemoSeanceController(DataSource source, JdbcClient jdbc, SessionQuizRepository sessions,
                         QuestionRepository questions, SimpMessagingTemplate messages, SeanceMapper mapper,
                         Clock horloge) {
        this.source = source;
        this.jdbc = jdbc;
        this.sessions = sessions;
        this.questions = questions;
        this.messages = messages;
        this.mapper = mapper;
        this.horloge = horloge;
    }

    /** Message {@code /topic/sessions/{codeAcces}/etat} (API.md §4). */
    record EtatMessage(String etat, int position, Long idQuestion, OffsetDateTime echeance) {
    }

    /** Recrée la séance de démonstration et ouvre sa première question. */
    @Override
    public void run(ApplicationArguments arguments) {
        new ResourceDatabasePopulator(new ClassPathResource("demo/seance-demo.sql")).execute(source);
        QuestionResponse question = ouvrir(CODE_DEMO, 1).getBody();
        journal.warn("demo_prete : séance {} (jeton « jeton-demo »), question {} ouverte jusqu'à {}", CODE_DEMO,
                question.idQuestion(), question.echeance());
    }

    /** Ouvre la question de rang {@code position} (1, 2, 3…) pour toute sa durée, comme le fera l'UC7. */
    @PostMapping("/questions/{position}")
    @Transactional
    ResponseEntity<QuestionResponse> ouvrir(@PathVariable String codeAcces, @PathVariable int position) {
        jdbc.sql("UPDATE session_quiz SET etat = 'EN_COURS', position_courante = :p, echeance_question = NULL "
                        + "WHERE code_acces = :c")
                .param("p", position).param("c", codeAcces).update();
        SessionQuiz session = sessions.trouverParCodeAcces(codeAcces).orElseThrow(SessionIntrouvableException::new);
        if (session.idQuestionOuverte() == null) {
            return ResponseEntity.notFound().build();
        }
        Question question = questions.trouverParId(session.idQuestionOuverte())
                .orElseThrow(SessionIntrouvableException::new);
        LocalDateTime echeance = LocalDateTime.now(horloge).plusSeconds(question.dureeSec());
        jdbc.sql("UPDATE session_quiz SET echeance_question = :e WHERE code_acces = :c")
                .param("e", echeance).param("c", codeAcces).update();

        SessionQuiz ouverte = sessions.trouverParCodeAcces(codeAcces).orElseThrow();
        QuestionResponse message = mapper.versResponse(LireEtatSeanceUseCase.pourEtudiant(question, ouverte));
        messages.convertAndSend("/topic/sessions/" + codeAcces + "/etat", new EtatMessage("EN_COURS", position,
                question.idQuestion(), mapper.avecDecalage(echeance)));
        messages.convertAndSend("/topic/sessions/" + codeAcces + "/question", message);
        return ResponseEntity.ok(message);
    }

    /** Clôt la séance : les réponses sont refusées, le front affiche la fin. */
    @PostMapping("/cloture")
    ResponseEntity<Void> clore(@PathVariable String codeAcces) {
        jdbc.sql("UPDATE session_quiz SET etat = 'CLOTUREE', echeance_question = NULL, date_cloture = :m "
                        + "WHERE code_acces = :c")
                .param("m", LocalDateTime.now(horloge)).param("c", codeAcces).update();
        messages.convertAndSend("/topic/sessions/" + codeAcces + "/etat",
                new EtatMessage("CLOTUREE", 0, null, null));
        return ResponseEntity.noContent().build();
    }
}
