package fr.iutmontreuil.quizzard.features.cours.infrastructure.demo;

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
 * <b>Démonstration seulement</b> (profil Spring {@code demo-uc2}) : crée au démarrage l'enseignant de
 * {@code demo/import-demo.sql}, que l'application ne sait pas encore connecter, pour importer un PDF avec la route
 * de l'UC2. Les cours importés sont gardés d'un démarrage à l'autre.
 * <p>
 * À lancer avec {@code SECURITE_OUVERTE=true}, tant que la connexion n'existe pas. Ce bean n'existe pas hors du
 * profil. À supprimer quand la feature {@code compte} sera faite.
 */
@Profile("demo-uc2")
@Component
class DonneesDemoImport implements ApplicationRunner {

    private static final Logger journal = LoggerFactory.getLogger(DonneesDemoImport.class);

    private final DataSource source;

    DonneesDemoImport(DataSource source) {
        this.source = source;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        new ResourceDatabasePopulator(new ClassPathResource("demo/import-demo.sql")).execute(source);
        journal.warn("demo_uc2_prete : importer un PDF par POST /api/cours");
    }
}
