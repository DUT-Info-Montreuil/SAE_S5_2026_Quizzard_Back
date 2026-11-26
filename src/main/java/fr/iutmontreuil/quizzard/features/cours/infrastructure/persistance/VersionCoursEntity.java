package fr.iutmontreuil.quizzard.features.cours.infrastructure.persistance;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Table {@code version_cours}. Le cours est référencé par son identifiant seulement. */
@Entity
@Table(name = "version_cours")
class VersionCoursEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_version")
    Long idVersion;

    @Column(name = "numero", nullable = false)
    int numero;

    @Column(name = "nom_fichier", nullable = false, length = 255)
    String nomFichier;

    @Column(name = "chemin_stockage", length = 500)
    String cheminStockage;

    @Column(name = "taille_octets", nullable = false)
    long tailleOctets;

    @Column(name = "nb_pages", nullable = false)
    int nbPages;

    /** SHA-256 du fichier ; la colonne est un {@code CHAR(64)}. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "empreinte", nullable = false, length = 64)
    String empreinte;

    @Column(name = "date_import", nullable = false)
    LocalDateTime dateImport;

    @Column(name = "date_suppression_fichier")
    LocalDateTime dateSuppressionFichier;

    @Column(name = "id_cours", nullable = false)
    Long idCours;

    protected VersionCoursEntity() {
        // Requis par JPA.
    }
}
