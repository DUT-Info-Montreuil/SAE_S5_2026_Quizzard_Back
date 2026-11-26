package fr.iutmontreuil.quizzard.features.cours.application.port;

import java.util.List;
import java.util.Optional;

import fr.iutmontreuil.quizzard.features.cours.domain.PageInseree;
import fr.iutmontreuil.quizzard.features.cours.domain.PagePlacee;
import fr.iutmontreuil.quizzard.features.cours.domain.PageQuiz;

/** Pages insérées (quiz ou classement) d'une version du cours. */
public interface PageInsereeRepository {

    /** Nombre de pages déjà insérées après cette page du PDF. */
    int compterAuMemeEndroit(Long idVersion, int numeroPage);

    /** Décale d'un rang les pages de la version au même endroit et de rang supérieur ou égal (une requête UPDATE). */
    void decalerOrdres(Long idVersion, int numeroPage, int aPartirDe);

    /** Enregistre la page (écriture forcée) et renseigne son identifiant. */
    PageQuiz enregistrer(PageQuiz page);

    /** Page insérée avec sa version et son cours, pour une modification ; vide si elle n'existe pas. */
    Optional<PagePlacee> trouverParId(Long idPage);

    /** Nombre de pages insérées après cette page du PDF, sans compter la page {@code idPageExclue}. */
    int compterAuMemeEndroitSauf(Long idVersion, int numeroPage, Long idPageExclue);

    /** Décale d'un rang les pages au même endroit et de rang supérieur ou égal, sauf {@code idPageExclue}. */
    void decalerOrdresSauf(Long idVersion, int numeroPage, int aPartirDe, Long idPageExclue);

    /** Place la page à l'emplacement donné. */
    void placer(Long idPage, int numeroPage, int ordre);

    /** Retire la page (sa question n'est pas supprimée). */
    void supprimer(Long idPage);

    /**
     * Renumérote les rangs des pages au même endroit, consécutifs à partir de 0 dans leur ordre actuel, sans compter
     * la page {@code idPageExclue} (nulle : aucune).
     */
    void recaler(Long idVersion, int numeroPage, Long idPageExclue);

    /** Pages insérées de la version, dans l'ordre d'affichage (page du PDF, puis rang). */
    List<PageInseree> listerParVersion(Long idVersion);
}
