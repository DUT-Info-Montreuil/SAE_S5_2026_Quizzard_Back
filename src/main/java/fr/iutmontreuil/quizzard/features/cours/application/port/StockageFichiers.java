package fr.iutmontreuil.quizzard.features.cours.application.port;

import java.io.InputStream;
import java.nio.file.Path;

import fr.iutmontreuil.quizzard.features.cours.application.StockageFichierException;
import fr.iutmontreuil.quizzard.features.cours.application.dto.FichierTemporaire;

/** Rangement des fichiers importés, hors de l'espace servi au public. */
public interface StockageFichiers {

    /**
     * Écrit le contenu reçu dans un fichier temporaire et calcule sa taille réelle et son empreinte SHA-256.
     *
     * @throws StockageFichierException si le fichier ne peut pas être lu ou écrit
     */
    FichierTemporaire ecrireTemporaire(InputStream contenu);

    /**
     * Range le fichier à son emplacement définitif. Si une transaction est en cours, le fichier rangé est supprimé
     * quand elle est annulée.
     *
     * @param cheminStockage chemin relatif choisi par le cas d'usage, enregistré en base
     * @throws StockageFichierException si le fichier ne peut pas être déplacé
     */
    void deplacer(FichierTemporaire fichier, String cheminStockage);

    /**
     * Supprime un fichier temporaire. Sans effet s'il n'existe plus ; un échec est journalisé et le fichier restant
     * sera traité comme orphelin (UC20).
     */
    void supprimer(Path chemin);
}
