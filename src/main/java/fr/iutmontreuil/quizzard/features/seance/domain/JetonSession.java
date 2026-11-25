package fr.iutmontreuil.quizzard.features.seance.domain;

/**
 * Jeton d'accès à une séance : le code secret qui figure dans le lien et le QR code
 * ({@code session_quiz.code_acces}). Sa valeur n'apparaît pas dans {@code toString()}, pour qu'elle ne finisse pas
 * dans un log.
 */
public record JetonSession(String valeur) {

    /** Taille de la colonne {@code session_quiz.code_acces}. */
    public static final int LONGUEUR_MAX = 32;

    public JetonSession {
        if (valeur == null || valeur.isBlank() || valeur.length() > LONGUEUR_MAX) {
            throw new IllegalArgumentException("jeton de séance vide ou de plus de " + LONGUEUR_MAX + " caractères");
        }
    }

    @Override
    public String toString() {
        return "JetonSession[masqué]";
    }
}
