package fr.iutmontreuil.quizzard.features.seance.infrastructure.persistance;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Table {@code participant}. La séance est référencée par son identifiant seulement : le participant et la séance
 * sont chargés séparément par leurs dépôts. L'identité d'un quiz noté ({@code identifiant_ldap}, {@code nom},
 * {@code prenom}) reste vide pour une entrée sans compte.
 */
@Entity
@Table(name = "participant")
class ParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_participant")
    Long idParticipant;

    @Column(name = "pseudonyme", nullable = false, length = 20)
    String pseudonyme;

    @Column(name = "identifiant_ldap", length = 64)
    String identifiantLdap;

    @Column(name = "nom", length = 100)
    String nom;

    @Column(name = "prenom", length = 100)
    String prenom;

    @Column(name = "identite_verifiee", nullable = false)
    boolean identiteVerifiee;

    @Column(name = "score_total", nullable = false)
    int scoreTotal;

    @Column(name = "date_entree", nullable = false)
    LocalDateTime dateEntree;

    /** Empreinte SHA-256 du jeton (64 caractères hexadécimaux) ; la colonne est un {@code CHAR(64)}. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "jeton_reconnexion", nullable = false, length = 64)
    String jetonReconnexion;

    @Column(name = "derniere_activite", nullable = false)
    LocalDateTime derniereActivite;

    @Column(name = "id_session", nullable = false)
    Long idSession;

    protected ParticipantEntity() {
        // Requis par JPA.
    }
}
