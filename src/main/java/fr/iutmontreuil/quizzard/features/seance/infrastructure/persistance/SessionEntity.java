package fr.iutmontreuil.quizzard.features.seance.infrastructure.persistance;

import java.time.LocalDateTime;

import fr.iutmontreuil.quizzard.features.seance.domain.EtatSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Table {@code session_quiz}, telle qu'écrite par l'enseignant qui anime la séance (UC7). Le quiz et la version sont
 * référencés par leur identifiant seulement ; ce qui ne change plus après le lancement n'est jamais réécrit.
 */
@Entity
@Table(name = "session_quiz")
class SessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_session")
    Long idSession;

    @Column(name = "code_acces", nullable = false, length = 32, updatable = false)
    String codeAcces;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat", nullable = false, length = 12)
    EtatSession etat;

    @Column(name = "est_notee", nullable = false, updatable = false)
    boolean estNotee;

    @Column(name = "date_lancement", updatable = false)
    LocalDateTime dateLancement;

    @Column(name = "date_cloture")
    LocalDateTime dateCloture;

    @Column(name = "position_courante", nullable = false)
    int positionCourante;

    @Column(name = "echeance_question")
    LocalDateTime echeanceQuestion;

    @Column(name = "id_quiz", nullable = false, updatable = false)
    Long idQuiz;

    @Column(name = "id_version", nullable = false, updatable = false)
    Long idVersion;

    @Column(name = "identite_non_verifiee_autorisee", nullable = false)
    boolean identiteNonVerifieeAutorisee;

    protected SessionEntity() {
        // Requis par JPA.
    }
}
