package fr.iutmontreuil.quizzard.features.cours.infrastructure.persistance;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.iutmontreuil.quizzard.features.cours.application.port.ImportCoursRepository;
import fr.iutmontreuil.quizzard.features.cours.domain.CoursImporte;

/**
 * Adaptateur du port {@link ImportCoursRepository}. L'écriture est forcée ({@code saveAndFlush}) pour qu'une erreur de
 * base soit détectée dans le cas d'usage, donc avant le commit.
 */
@Repository
class PostgresImportCoursRepository implements ImportCoursRepository {

    private final CoursJpaRepository cours;
    private final VersionCoursJpaRepository versions;
    private final JdbcClient jdbc;

    PostgresImportCoursRepository(CoursJpaRepository cours, VersionCoursJpaRepository versions, JdbcClient jdbc) {
        this.cours = cours;
        this.versions = versions;
        this.jdbc = jdbc;
    }

    @Override
    public void verrouillerImports(Long idEnseignant) {
        // NO KEY UPDATE : bloque un autre import, pas les insertions qui référencent l'enseignant.
        jdbc.sql("SELECT id_enseignant FROM enseignant WHERE id_enseignant = :idEnseignant FOR NO KEY UPDATE")
                .param("idEnseignant", idEnseignant)
                .query(Long.class)
                .list();
    }

    @Override
    public Optional<CoursImporte> trouverParEmpreinte(Long idEnseignant, String empreinte) {
        return cours.trouverNonArchivesParEmpreinte(idEnseignant, empreinte, Limit.of(1)).stream()
                .findFirst()
                .map(c -> CoursEntityMapper.versDomaine(c, versions.findByIdCoursOrderByNumero(c.idCours)));
    }

    @Override
    public CoursImporte enregistrer(CoursImporte nouveau) {
        CoursEntity enregistre = cours.saveAndFlush(CoursEntityMapper.versEntite(nouveau));
        List<VersionCoursEntity> enregistrees = versions.saveAllAndFlush(nouveau.versions().stream()
                .map(version -> CoursEntityMapper.versEntite(version, enregistre.idCours))
                .toList());
        return CoursEntityMapper.versDomaine(enregistre, enregistrees);
    }
}
