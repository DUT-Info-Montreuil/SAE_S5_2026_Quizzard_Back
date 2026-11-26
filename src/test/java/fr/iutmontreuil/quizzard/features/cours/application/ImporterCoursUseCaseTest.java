package fr.iutmontreuil.quizzard.features.cours.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.iutmontreuil.quizzard.features.cours.application.dto.ImporterCoursDto;
import fr.iutmontreuil.quizzard.features.cours.domain.CoursImporte;
import fr.iutmontreuil.quizzard.features.cours.domain.DoublonCoursException;
import fr.iutmontreuil.quizzard.features.cours.domain.FichierNonPdfException;
import fr.iutmontreuil.quizzard.features.cours.domain.TitreInvalideException;
import fr.iutmontreuil.quizzard.features.cours.domain.VersionImportee;

class ImporterCoursUseCaseTest {

    private final DoublesImport doubles = new DoublesImport();

    @Test
    void l_import_cree_le_cours_dans_l_ordre_du_dossier_et_supprime_le_fichier_temporaire() {
        CoursImporte cours = importer("  Réseaux ", false);

        assertThat(cours.idCours()).isEqualTo(11L);
        assertThat(cours.titre()).isEqualTo("Réseaux");
        assertThat(cours.idEnseignant()).isEqualTo(1L);
        VersionImportee version = cours.derniereVersion();
        assertThat(version.numero()).isEqualTo(1);
        assertThat(version.nbPages()).isEqualTo(12);
        assertThat(version.tailleOctets()).isEqualTo(2048);
        assertThat(version.empreinte()).isEqualTo(DoublesImport.EMPREINTE);
        assertThat(version.nomFichier()).isEqualTo("chapitre.pdf");
        assertThat(version.dateImport()).isEqualTo(LocalDateTime.of(2026, 11, 25, 14, 0, 0, 123_000_000));
        assertThat(version.cheminStockage()).isEqualTo(doubles.stockage.cheminDeRangement)
                .matches("cours/[0-9a-f-]{36}\\.pdf");
        assertThat(doubles.appels).containsExactly("ecrireTemporaire", "analyser", "verrou", "chercherDoublon",
                "deplacer", "enregistrer", "journal", "supprimer");
        assertThat(doubles.stockage.supprimes).containsExactly(DoublesImport.TEMPORAIRE);
    }

    @Test
    void l_import_est_une_action_sensible_sans_titre_ni_nom_de_fichier() {
        importer("Titre secret", false);

        assertThat(doubles.journal.lignes).singleElement().satisfies(ligne -> {
            assertThat(ligne.type()).isEqualTo("actionSensible");
            assertThat(ligne.nom()).isEqualTo("cours_importe");
            assertThat(ligne.champs()).containsEntry("cible", 11L).containsEntry("idVersion", 21L)
                    .containsEntry("nbPages", 12).containsEntry("tailleOctets", 2048L);
            assertThat(ligne.champs().toString()).doesNotContain("Titre secret").doesNotContain("chapitre");
        });
    }

    @Test
    void un_titre_refuse_n_ecrit_rien_sur_le_disque() {
        assertThatThrownBy(() -> importer(" ", false)).isInstanceOf(TitreInvalideException.class);

        assertThat(doubles.appels).isEmpty();
    }

    @Test
    void un_fichier_refuse_par_l_analyse_ne_cree_rien() {
        doubles.refusAnalyse = new FichierNonPdfException();

        assertThatThrownBy(() -> importer("Réseaux", false)).isInstanceOf(FichierNonPdfException.class);

        assertThat(doubles.appels).containsExactly("ecrireTemporaire", "analyser", "supprimer");
    }

    @Test
    void un_doublon_non_confirme_est_refuse_et_trace_dans_les_logs_seulement() {
        doubles.depot.existant = coursExistant();

        assertThatThrownBy(() -> importer("Réseaux", false))
                .isInstanceOf(DoublonCoursException.class)
                .hasMessage("Vous avez déjà importé ce PDF dans le cours « Ancien cours ». Importer quand même ?");

        assertThat(doubles.appels).doesNotContain("deplacer", "enregistrer", "journal").endsWith("supprimer");
        assertThat(doubles.journal.lignes).singleElement().satisfies(ligne -> {
            assertThat(ligne.type()).isEqualTo("regleRefusee");
            assertThat(ligne.nom()).isEqualTo("import_refuse_doublon");
            assertThat(ligne.champs()).containsOnly(org.assertj.core.api.Assertions.entry("idCoursExistant", 4L));
        });
    }

    @Test
    void un_doublon_confirme_cree_un_second_cours() {
        doubles.depot.existant = coursExistant();

        assertThat(importer("Réseaux", true).idCours()).isEqualTo(11L);
    }

    @Test
    void une_panne_de_la_base_supprime_le_temporaire_et_ne_trace_pas_l_import() {
        doubles.depot.panne = new IllegalStateException("base");

        assertThatThrownBy(() -> importer("Réseaux", false)).isInstanceOf(IllegalStateException.class);

        assertThat(doubles.appels).doesNotContain("journal").endsWith("supprimer");
    }

    private CoursImporte importer(String titre, boolean confirmer) {
        return doubles.casDUsage().executer(new ImporterCoursDto(titre, "chapitre.pdf",
                new ByteArrayInputStream(new byte[0]), 1L, confirmer));
    }

    private static CoursImporte coursExistant() {
        LocalDateTime avant = LocalDateTime.of(2026, 10, 1, 8, 0);
        VersionImportee version = VersionImportee.premiere("a.pdf", "cours/a.pdf", 1, 1, DoublesImport.EMPREINTE,
                avant);
        return new CoursImporte(4L, "Ancien cours", 1L, false, avant, avant, List.of(version));
    }
}
