package fr.iutmontreuil.quizzard.features.cours.infrastructure.persistance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
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
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;
import fr.iutmontreuil.quizzard.features.cours.FixturesImport;
import fr.iutmontreuil.quizzard.features.cours.FixturesImport.Enseignant;
import fr.iutmontreuil.quizzard.features.cours.PdfDeTest;
import fr.iutmontreuil.quizzard.features.cours.application.ImporterCoursUseCase;
import fr.iutmontreuil.quizzard.features.cours.application.dto.ImporterCoursDto;
import fr.iutmontreuil.quizzard.features.cours.domain.DoublonCoursException;

/** Double clic sur « Importer » : un seul cours, le second envoi demande la confirmation. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ImportConcurrentTest {

    @TempDir
    static Path stockage;

    @DynamicPropertySource
    static void dossierDeStockage(DynamicPropertyRegistry proprietes) {
        proprietes.add("quizzard.stockage.dossier", stockage::toString);
    }

    @Autowired
    ImporterCoursUseCase importerCours;
    @Autowired
    JdbcClient jdbc;

    FixturesImport fixtures;

    @BeforeEach
    void preparer() {
        fixtures = new FixturesImport(jdbc);
    }

    @AfterEach
    void nettoyer() {
        fixtures.nettoyer();
    }

    @RepeatedTest(3)
    void de_deux_imports_simultanes_du_meme_pdf_un_seul_reussit() throws Exception {
        Enseignant enseignant = fixtures.creerEnseignant();
        byte[] pdf = PdfDeTest.pages(20);
        CountDownLatch depart = new CountDownLatch(1);
        List<Throwable> refus = new ArrayList<>();

        try (ExecutorService fils = Executors.newFixedThreadPool(2)) {
            List<Future<?>> imports = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                imports.add(fils.submit(() -> {
                    depart.await();
                    return importerCours.executer(new ImporterCoursDto("Réseaux", "cours.pdf",
                            new ByteArrayInputStream(pdf), enseignant.id(), false));
                }));
            }
            depart.countDown();
            for (Future<?> envoi : imports) {
                try {
                    envoi.get();
                } catch (ExecutionException e) {
                    refus.add(e.getCause());
                }
            }
        }

        assertThat(refus).singleElement().isInstanceOf(DoublonCoursException.class);
        assertThat(fixtures.nombreDeCours(enseignant)).isEqualTo(1);
    }
}
