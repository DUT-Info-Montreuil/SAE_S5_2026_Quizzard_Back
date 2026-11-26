package fr.iutmontreuil.quizzard.features.seance.application.dto;

import java.util.Arrays;

/**
 * Image PNG du QR code du lien d'accès à une séance. Elle contient le code d'accès : {@code toString()} n'en montre
 * que la taille.
 */
public record QrCode(byte[] png) {

    public QrCode {
        if (png == null || png.length == 0) {
            throw new IllegalArgumentException("image du QR code vide");
        }
        png = png.clone();
    }

    @Override
    public byte[] png() {
        return png.clone();
    }

    @Override
    public boolean equals(Object autre) {
        return autre instanceof QrCode qr && Arrays.equals(png, qr.png);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(png);
    }

    @Override
    public String toString() {
        return "QrCode[" + png.length + " octets]";
    }
}
