package fr.iutmontreuil.quizzard.features.cours.application.port;

import java.util.List;

import fr.iutmontreuil.quizzard.features.cours.domain.PageInseree;
import fr.iutmontreuil.quizzard.features.cours.domain.PageQuiz;

/** Pages insérées (quiz ou classement) d'une version du cours. */
public interface PageInsereeRepository {

    /** Nombre de pages déjà insérées après cette page du PDF. */
    int compterAuMemeEndroit(Long idVersion, int numeroPage);

    /** Décale d'un rang les pages de la version au même endroit et de rang supérieur ou égal (une requête UPDATE). */
    void decalerOrdres(Long idVersion, int numeroPage, int aPartirDe);

    /** Enregistre la page (écriture forcée) et renseigne son identifiant. */
    PageQuiz enregistrer(PageQuiz page);

    /** Pages insérées de la version, dans l'ordre d'affichage (page du PDF, puis rang). */
    List<PageInseree> listerParVersion(Long idVersion);
}
