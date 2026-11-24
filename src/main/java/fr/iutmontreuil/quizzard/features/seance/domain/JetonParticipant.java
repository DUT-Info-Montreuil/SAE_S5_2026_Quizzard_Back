package fr.iutmontreuil.quizzard.features.seance.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Jeton secret remis au participant à son entrée. La base ne garde que son empreinte SHA-256 en hexadécimal
 * (64 caractères, colonne {@code participant.jeton_reconnexion}) : un jeton volé dans la base est inutilisable.
 */
public final class JetonParticipant {

    private JetonParticipant() {
    }

    /** Empreinte SHA-256 du jeton, en hexadécimal minuscule. */
    public static String empreinte(String jeton) {
        try {
            byte[] octets = MessageDigest.getInstance("SHA-256").digest(jeton.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(octets);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 est fourni par tout JDK", e);
        }
    }
}
