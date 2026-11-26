package fr.iutmontreuil.quizzard.features.seance.infrastructure.qrcode;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import fr.iutmontreuil.quizzard.features.seance.application.GenerationAccesException;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QrCode;
import fr.iutmontreuil.quizzard.features.seance.application.port.GenerateurQrCode;

/**
 * QR code du lien d'accès, en PNG noir et blanc de 512 pixels, avec la marge blanche de 4 modules qu'attendent les
 * lecteurs. Correction d'erreur moyenne (15 %) : il reste lisible projeté en salle.
 */
@Component
class ZxingGenerateurQrCode implements GenerateurQrCode {

    static final int TAILLE = 512;
    private static final int NOIR = 0xFF000000;
    private static final int BLANC = 0xFFFFFFFF;

    @Override
    public QrCode generer(String lienAcces) {
        try {
            BitMatrix modules = new QRCodeWriter().encode(lienAcces, BarcodeFormat.QR_CODE, TAILLE, TAILLE, Map.of(
                    EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                    EncodeHintType.CHARACTER_SET, "UTF-8",
                    EncodeHintType.MARGIN, 4));
            BufferedImage image = new BufferedImage(modules.getWidth(), modules.getHeight(),
                    BufferedImage.TYPE_BYTE_BINARY);
            for (int x = 0; x < modules.getWidth(); x++) {
                for (int y = 0; y < modules.getHeight(); y++) {
                    image.setRGB(x, y, modules.get(x, y) ? NOIR : BLANC);
                }
            }
            ByteArrayOutputStream sortie = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", sortie)) {
                throw new GenerationAccesException("Aucun encodeur PNG disponible.", null);
            }
            return new QrCode(sortie.toByteArray());
        } catch (WriterException | IOException | IllegalArgumentException e) {
            // Le lien contient le code d'accès : il n'est pas recopié dans le message.
            throw new GenerationAccesException("QR code impossible à produire.", e);
        }
    }
}
