package fr.iutmontreuil.quizzard.features.cours.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import fr.iutmontreuil.quizzard.features.cours.application.dto.CoursConsulte;
import fr.iutmontreuil.quizzard.features.cours.application.port.ConsultationCoursRepository;
import fr.iutmontreuil.quizzard.features.cours.domain.CoursIntrouvableException;

/** Liste et ouverture des cours : tout est demandé pour le seul enseignant connecté. */
class ConsultationCoursUseCaseTest {

    private static final Long PROPRIETAIRE = 7L;
    private static final CoursConsulte COURS = new CoursConsulte(11L, "Réseaux", 21L, 1, 12, 0,
            LocalDateTime.of(2026, 11, 25, 14, 0));

    /** Doublure en mémoire : un seul cours, celui de {@link #PROPRIETAIRE}. */
    private final ConsultationCoursRepository depot = new ConsultationCoursRepository() {
        @Override
        public List<CoursConsulte> listerParEnseignant(Long idEnseignant) {
            return PROPRIETAIRE.equals(idEnseignant) ? List.of(COURS) : List.of();
        }

        @Override
        public Optional<CoursConsulte> trouverParEnseignant(Long idCours, Long idEnseignant) {
            return listerParEnseignant(idEnseignant).stream().filter(c -> c.idCours().equals(idCours)).findFirst();
        }
    };

    @Test
    void la_liste_est_celle_de_l_enseignant_demande() {
        ListerCoursUseCase lister = new ListerCoursUseCase(depot);

        assertThat(lister.executer(PROPRIETAIRE)).containsExactly(COURS);
        assertThat(lister.executer(8L)).isEmpty();
    }

    @Test
    void ouvrir_renvoie_le_cours_de_son_proprietaire() {
        assertThat(new OuvrirCoursUseCase(depot).executer(11L, PROPRIETAIRE)).isEqualTo(COURS);
    }

    @Test
    void ouvrir_un_cours_inconnu_ou_d_un_autre_enseignant_est_refuse() {
        OuvrirCoursUseCase ouvrir = new OuvrirCoursUseCase(depot);

        assertThatThrownBy(() -> ouvrir.executer(11L, 8L)).isInstanceOf(CoursIntrouvableException.class);
        assertThatThrownBy(() -> ouvrir.executer(99L, PROPRIETAIRE)).isInstanceOf(CoursIntrouvableException.class);
    }

    @Test
    void le_titre_n_apparait_pas_dans_la_forme_texte_d_un_cours() {
        assertThat(COURS.toString()).doesNotContain("Réseaux");
    }
}
