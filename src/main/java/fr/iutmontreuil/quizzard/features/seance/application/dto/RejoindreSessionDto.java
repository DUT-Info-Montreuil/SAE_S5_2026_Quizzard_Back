package fr.iutmontreuil.quizzard.features.seance.application.dto;

/**
 * Commande « rejoindre une séance » : le code d'accès (lu dans l'adresse) et le pseudonyme saisi, tel quel,
 * avant nettoyage. {@link #toString()} ne montre rien : ni le pseudonyme (donnée personnelle) ni le code d'accès
 * (secret du lien) ne doivent passer dans un log.
 */
public record RejoindreSessionDto(String codeAcces, String pseudonyme) {

    @Override
    public String toString() {
        return "RejoindreSessionDto[masqué]";
    }
}
