package fr.iutmontreuil.quizzard.features.cours.infrastructure.persistance;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Table {@code cours}. L'enseignant est référencé par son identifiant seulement. */
@Entity
@Table(name = "cours")
class CoursEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cours")
    Long idCours;

    @Column(name = "titre", nullable = false, length = 200)
    String titre;

    @Column(name = "date_creation", nullable = false)
    LocalDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    LocalDateTime dateModification;

    @Column(name = "archive", nullable = false)
    boolean archive;

    @Column(name = "id_enseignant", nullable = false)
    Long idEnseignant;

    protected CoursEntity() {
        // Requis par JPA.
    }
}
