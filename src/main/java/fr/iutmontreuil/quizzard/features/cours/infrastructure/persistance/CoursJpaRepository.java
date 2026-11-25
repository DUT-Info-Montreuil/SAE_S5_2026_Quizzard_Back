package fr.iutmontreuil.quizzard.features.cours.infrastructure.persistance;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CoursJpaRepository extends JpaRepository<CoursEntity, Long> {

    /** Cours non archivés de l'enseignant dont une version a cette empreinte, le plus récent d'abord. */
    @Query("""
            select c from CoursEntity c
             where c.idEnseignant = :idEnseignant and c.archive = false
               and exists (select v.idVersion from VersionCoursEntity v
                            where v.idCours = c.idCours and v.empreinte = :empreinte)
             order by c.idCours desc
            """)
    List<CoursEntity> trouverNonArchivesParEmpreinte(@Param("idEnseignant") Long idEnseignant,
                                                     @Param("empreinte") String empreinte, Limit limite);
}
