package fr.iutmontreuil.quizzard.features.seance.infrastructure.demo;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/**
 * <b>Démonstration seulement</b> (profil Spring {@code demo-uc7}) : crée au démarrage un enseignant, un cours et le
 * quiz {@code 910000} (données de {@code demo/lancement-demo.sql}), que l'application ne sait pas encore créer. Le
 * lancement et le déroulement passent par les routes de l'UC7.
 * <p>
 * À lancer avec {@code SECURITE_OUVERTE=true}, tant que la connexion n'existe pas. Ce bean n'existe pas hors du
 * profil. À supprimer quand les quiz pourront être créés dans l'application.
 */
@Profile("demo-uc7")
@Component
class DonneesDemoLancement implements ApplicationRunner {

    static final long ID_QUIZ = 910000;

    private static final Logger journal = LoggerFactory.getLogger(DonneesDemoLancement.class);

    private final DataSource source;

    DonneesDemoLancement(DataSource source) {
        this.source = source;
    }

    /** Recrée les données de démonstration, séances de ce quiz comprises. */
    @Override
    public void run(ApplicationArguments arguments) {
        new ResourceDatabasePopulator(new ClassPathResource("demo/lancement-demo.sql")).execute(source);
        journal.warn("demo_uc7_prete : lancer le quiz {} par POST /api/quiz/{}/sessions", ID_QUIZ, ID_QUIZ);
    }
}
