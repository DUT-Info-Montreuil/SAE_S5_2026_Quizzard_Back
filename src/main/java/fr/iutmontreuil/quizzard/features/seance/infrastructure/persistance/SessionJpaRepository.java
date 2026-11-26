package fr.iutmontreuil.quizzard.features.seance.infrastructure.persistance;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fr.iutmontreuil.quizzard.features.seance.domain.EtatSession;
import jakarta.persistence.LockModeType;

interface SessionJpaRepository extends JpaRepository<SessionEntity, Long> {

    /** La séance, ligne verrouillée jusqu'à la fin de la transaction ({@code SELECT … FOR UPDATE}). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessionEntity s where s.idSession = :idSession")
    Optional<SessionEntity> verrouiller(@Param("idSession") Long idSession);

    List<SessionEntity> findByEtatAndEcheanceQuestionIsNotNull(EtatSession etat);
}
