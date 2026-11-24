package fr.iutmontreuil.quizzard.features.cours.domain;

/**
 * Cours d'un enseignant, vu pour une modification : seule sa dernière version accepte de nouvelles pages.
 *
 * @param derniereVersion version de numéro le plus élevé
 */
public record Cours(Long idCours, Long idEnseignant, boolean archive, VersionCours derniereVersion) {

    /**
     * Vérifie que le cours est modifiable et que la version visée est sa dernière version.
     *
     * @return la dernière version
     * @throws CoursArchiveException   si le cours est archivé
     * @throws VersionPerimeeException si la version visée n'est pas la dernière (ou n'est pas de ce cours)
     */
    public VersionCours versionModifiable(Long idVersion) {
        if (archive) {
            throw new CoursArchiveException();
        }
        if (derniereVersion == null || !derniereVersion.idVersion().equals(idVersion)) {
            throw new VersionPerimeeException();
        }
        return derniereVersion;
    }
}
