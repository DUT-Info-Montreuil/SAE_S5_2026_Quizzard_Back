package fr.iutmontreuil.quizzard.features.seance.application.port;

import fr.iutmontreuil.quizzard.features.seance.application.GenerationAccesException;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QrCode;

/** QR code produit par l'application, sans service externe. */
public interface GenerateurQrCode {

    /** @throws GenerationAccesException si l'image ne peut pas être produite */
    QrCode generer(String lienAcces);
}
