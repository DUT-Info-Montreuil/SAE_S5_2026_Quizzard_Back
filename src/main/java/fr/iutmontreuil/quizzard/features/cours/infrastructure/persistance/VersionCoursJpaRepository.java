package fr.iutmontreuil.quizzard.features.cours.infrastructure.persistance;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface VersionCoursJpaRepository extends JpaRepository<VersionCoursEntity, Long> {

    List<VersionCoursEntity> findByIdCoursOrderByNumero(Long idCours);
}
