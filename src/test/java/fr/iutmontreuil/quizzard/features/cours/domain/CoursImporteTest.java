package fr.iutmontreuil.quizzard.features.cours.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CoursImporteTest {

    private static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 25, 14, 0);
    private static final String EMPREINTE = "a".repeat(64);

    @Test
    void un_cours_importe_a_sa_version_1_et_un_titre_sans_espaces_autour() {
        CoursImporte cours = CoursImporte.creer("  Réseaux  ", 1L, version("cours.pdf"), MAINTENANT);

        assertThat(cours.idCours()).isNull();
        assertThat(cours.titre()).isEqualTo("Réseaux");
        assertThat(cours.archive()).isFalse();
        assertThat(cours.dateCreation()).isEqualTo(MAINTENANT);
        assertThat(cours.dateModification()).isEqualTo(MAINTENANT);
        assertThat(cours.derniereVersion().numero()).isEqualTo(1);
        assertThat(cours.derniereVersion().dateSuppressionFichier()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n", "titre\0nul"})
    void un_titre_vide_ou_inutilisable_est_refuse(String titre) {
        assertThatThrownBy(() -> CoursImporte.creer(titre, 1L, version("cours.pdf"), MAINTENANT))
                .isInstanceOfSatisfying(TitreInvalideException.class, e -> {
                    assertThat(e.champ()).isEqualTo("titre");
                    assertThat(e.code()).isEqualTo("TITRE_INVALIDE");
                })
                .hasMessage("Le titre est obligatoire (200 caractères au plus).");
    }

    @Test
    void un_titre_de_200_caracteres_passe_et_de_201_non() {
        assertThat(CoursImporte.nettoyerTitre("é".repeat(200))).hasSize(200);
        // Un caractère hors du plan de base compte pour un seul, comme dans la colonne PostgreSQL.
        assertThat(CoursImporte.nettoyerTitre("😀".repeat(200))).isNotEmpty();
        assertThatThrownBy(() -> CoursImporte.nettoyerTitre("é".repeat(201)))
                .isInstanceOf(TitreInvalideException.class);
    }

    @Test
    void le_nom_du_fichier_perd_ses_dossiers_et_ses_caracteres_de_controle() {
        assertThat(version("C:\\Users\\prof\\Réseaux.pdf").nomFichier()).isEqualTo("Réseaux.pdf");
        assertThat(version("../../etc/passwd").nomFichier()).isEqualTo("passwd");
        assertThat(version("cours\r\n.pdf").nomFichier()).isEqualTo("cours.pdf");
        assertThat(version(null).nomFichier()).isEqualTo("cours.pdf");
        assertThat(version("dossier/").nomFichier()).isEqualTo("cours.pdf");
        assertThat(version("n".repeat(300) + ".pdf").nomFichier()).hasSize(VersionImportee.LONGUEUR_MAX_NOM);
    }

    @Test
    void des_valeurs_impossibles_apres_analyse_sont_des_erreurs_de_programmation() {
        assertThatThrownBy(() -> VersionImportee.premiere("a.pdf", "cours/a.pdf", 1, 0, EMPREINTE, MAINTENANT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VersionImportee.premiere("a.pdf", "cours/a.pdf", 1, 1, "abc", MAINTENANT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VersionImportee.premiere("a.pdf", " ", 1, 1, EMPREINTE, MAINTENANT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CoursImporte.creer("Titre", null, version("a.pdf"), MAINTENANT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ni_le_titre_ni_le_nom_du_fichier_ne_se_montrent() {
        CoursImporte cours = CoursImporte.creer("Titre secret", 1L, version("Dupont.pdf"), MAINTENANT);

        assertThat(cours.toString()).doesNotContain("Titre secret").doesNotContain("Dupont");
        assertThat(cours.derniereVersion().toString()).doesNotContain("Dupont");
    }

    private static VersionImportee version(String nomFichier) {
        return VersionImportee.premiere(nomFichier, "cours/x.pdf", 10, 3, EMPREINTE, MAINTENANT);
    }
}
