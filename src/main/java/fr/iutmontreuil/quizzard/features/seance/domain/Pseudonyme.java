package fr.iutmontreuil.quizzard.features.seance.domain;

import java.text.Normalizer;
import java.util.Set;

/**
 * Pseudonyme d'un participant (objet de valeur). Un pseudonyme existant est toujours valide : il est nettoyé,
 * fait de 2 à 20 caractères et ne contient aucun caractère de contrôle.
 * <p>
 * Nettoyage ({@link #de(String)}), dans cet ordre : normalisation Unicode NFC, suites d'espaces réduites à un seul,
 * espaces de début et de fin retirés. La longueur compte des caractères Unicode (un emoji vaut 1), comme
 * {@code VARCHAR(20)} dans PostgreSQL. L'unicité se compare à l'identique : « Alice » et « alice » sont deux
 * pseudonymes (règle à valider avec le client, docs/DESIGN.md §4).
 * <p>
 * Le pseudonyme est une donnée personnelle : {@link #toString()} ne le montre pas, pour ne jamais le laisser
 * passer dans un log.
 */
public record Pseudonyme(String valeur) {

    public static final int LONGUEUR_MIN = 2;
    public static final int LONGUEUR_MAX = 20;

    public Pseudonyme {
        if (valeur == null || !nettoyer(valeur).equals(valeur) || longueurInvalide(valeur) || contientControle(valeur)) {
            throw new PseudonymeInvalideException();
        }
    }

    /**
     * Nettoie le texte saisi puis vérifie qu'il forme un pseudonyme valide.
     *
     * @throws PseudonymeInvalideException si le texte est absent, contient un caractère de contrôle ou ne fait
     *                                     pas 2 à 20 caractères une fois nettoyé
     */
    public static Pseudonyme de(String texte) {
        if (texte == null) {
            throw new PseudonymeInvalideException();
        }
        return new Pseudonyme(nettoyer(texte));
    }

    /**
     * Premier pseudonyme libre obtenu en ajoutant 2, 3, 4… à la fin de celui-ci, en raccourcissant la racine
     * pour rester à 20 caractères : « Alice » donne « Alice2 ».
     *
     * @param pris les pseudonymes déjà utilisés dans la séance (300 au plus)
     */
    public Pseudonyme proposerAlternative(Set<String> pris) {
        int longueur = valeur.codePointCount(0, valeur.length());
        for (int rang = 2; ; rang++) {
            String suffixe = Integer.toString(rang);
            int longueurRacine = Math.min(longueur, LONGUEUR_MAX - suffixe.length());
            String racine = valeur.substring(0, valeur.offsetByCodePoints(0, longueurRacine)).stripTrailing();
            String candidat = racine + suffixe;
            if (!pris.contains(candidat)) {
                return new Pseudonyme(candidat);
            }
        }
    }

    private static String nettoyer(String texte) {
        return Normalizer.normalize(texte, Normalizer.Form.NFC)
                .replaceAll("\\p{Zs}+", " ")
                .strip();
    }

    private static boolean longueurInvalide(String texte) {
        int longueur = texte.codePointCount(0, texte.length());
        return longueur < LONGUEUR_MIN || longueur > LONGUEUR_MAX;
    }

    private static boolean contientControle(String texte) {
        return texte.codePoints().anyMatch(point -> Character.getType(point) == Character.CONTROL);
    }

    @Override
    public String toString() {
        return "Pseudonyme[masqué]";
    }
}
