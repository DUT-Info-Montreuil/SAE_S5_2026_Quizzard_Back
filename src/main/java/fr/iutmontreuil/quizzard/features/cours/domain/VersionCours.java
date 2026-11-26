package fr.iutmontreuil.quizzard.features.cours.domain;

/** Version importée du PDF d'un cours. */
public record VersionCours(Long idVersion, int numero, int nbPages) {

    /**
     * Un emplacement (numeroPage, ordre) est valide si {@code 0 ≤ numeroPage ≤ nbPages} (0 : avant la première page)
     * et {@code 0 ≤ ordre ≤ nbAuMemeEndroit} (nombre de pages déjà insérées au même endroit).
     *
     * @throws EmplacementInvalideException sinon
     */
    public void verifierEmplacement(int numeroPage, int ordre, int nbAuMemeEndroit) {
        if (numeroPage < 0 || numeroPage > nbPages || ordre < 0 || ordre > nbAuMemeEndroit) {
            throw new EmplacementInvalideException();
        }
    }
}
