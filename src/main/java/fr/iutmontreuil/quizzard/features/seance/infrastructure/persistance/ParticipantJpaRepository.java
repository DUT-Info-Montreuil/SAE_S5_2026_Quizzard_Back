package fr.iutmontreuil.quizzard.features.seance.infrastructure.persistance;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ParticipantJpaRepository extends JpaRepository<ParticipantEntity, Long> {

    /** Comparaison à l'identique (sensible à la casse), comme la contrainte {@code uq_participant_pseudonyme}. */
    boolean existsByIdSessionAndPseudonyme(Long idSession, String pseudonyme);

    long countByIdSession(Long idSession);

    @Query("select p.pseudonyme from ParticipantEntity p where p.idSession = :idSession")
    Set<String> pseudonymesDeLaSession(@Param("idSession") Long idSession);
}
