package fr.iutmontreuil.quizzard.features.seance.domain;

/**
 * Barème d'une question (DESIGN.md §3) : points maximum, décroissance avec le temps, points partiels.
 *
 * @param pointsMax         points d'une réponse juste et instantanée (1000 par défaut)
 * @param decroissanceTemps si vrai, une réponse à la dernière milliseconde ne vaut plus que la moitié des points
 * @param pointsPartiels    si vrai, une réponse partiellement juste rapporte une part des points
 */
public record Bareme(int pointsMax, boolean decroissanceTemps, boolean pointsPartiels) {

    public Bareme {
        if (pointsMax < 0) {
            throw new IllegalArgumentException("pointsMax doit être positif ou nul");
        }
    }

    /**
     * {@code arrondi(pointsMax × fractionJuste × facteurTemps)}, avec {@code facteurTemps = 1 − (t / T) / 2} si
     * la décroissance est active, 1 sinon. Le temps t est borné à [0, T].
     *
     * @param tempsMs       temps de réponse t, mesuré par le serveur
     * @param dureeMs       durée T de la question
     * @param fractionJuste part juste de la réponse, entre 0 et 1
     * @throws IllegalArgumentException si fractionJuste n'est pas entre 0 et 1 ou si la durée n'est pas positive
     */
    public int calculerPoints(long tempsMs, long dureeMs, double fractionJuste) {
        if (fractionJuste < 0 || fractionJuste > 1) {
            throw new IllegalArgumentException("fractionJuste doit être entre 0 et 1 : " + fractionJuste);
        }
        if (dureeMs <= 0) {
            throw new IllegalArgumentException("dureeMs doit être positive : " + dureeMs);
        }
        double facteurTemps = 1;
        if (decroissanceTemps) {
            long t = Math.clamp(tempsMs, 0, dureeMs);
            facteurTemps = 1 - ((double) t / dureeMs) / 2;
        }
        return (int) Math.round(pointsMax * fractionJuste * facteurTemps);
    }
}
