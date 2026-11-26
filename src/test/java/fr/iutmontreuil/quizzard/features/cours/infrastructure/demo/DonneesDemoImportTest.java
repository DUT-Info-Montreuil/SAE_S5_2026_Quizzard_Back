package fr.iutmontreuil.quizzard.features.cours.infrastructure.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import fr.iutmontreuil.quizzard.TestcontainersConfiguration;
import fr.iutmontreuil.quizzard.features.cours.PdfDeTest;

/** La démonstration permet d'importer un PDF sans connexion, et se rejoue au redémarrage sans perdre les cours. */
@SpringBootTest(properties = "quizzard.securite.ouverte=true")
@AutoConfigureMockMvc
@ActiveProfiles("demo-uc2")
@Import(TestcontainersConfiguration.class)
class DonneesDemoImportTest {

    @TempDir
    static Path stockage;

    @DynamicPropertySource
    static void dossierDeStockage(DynamicPropertyRegistry proprietes) {
        proprietes.add("quizzard.stockage.dossier", stockage::toString);
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    DonneesDemoImport demo;

    @Test
    void un_pdf_s_importe_sans_connexion_et_la_demonstration_se_rejoue() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "demo.pdf", "application/pdf",
                PdfDeTest.pages(2));
        mvc.perform(multipart("/api/cours").file(fichier).param("titre", "Démo"))
                .andExpect(status().isCreated());

        demo.run(new DefaultApplicationArguments());

        // Le cours est toujours là : le même PDF est un doublon.
        mvc.perform(multipart("/api/cours").file(fichier).param("titre", "Démo"))
                .andExpect(status().isConflict());
    }
}
