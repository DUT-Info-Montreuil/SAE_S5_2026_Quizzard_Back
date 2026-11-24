package fr.iutmontreuil.quizzard.features.cours.infrastructure.persistance;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.iutmontreuil.quizzard.features.cours.application.port.PageInsereeRepository;
import fr.iutmontreuil.quizzard.features.cours.domain.PageInseree;
import fr.iutmontreuil.quizzard.features.cours.domain.PageInseree.TypePage;
import fr.iutmontreuil.quizzard.features.cours.domain.PageQuiz;

/**
 * Pages insérées en SQL. Le décalage des rangs repose sur la contrainte de position différée (migration V2) :
 * l'unicité de (version, page, rang) n'est vérifiée qu'à la fin de la transaction.
 */
@Repository
class PostgresPageInsereeRepository implements PageInsereeRepository {

    private final JdbcClient jdbc;

    PostgresPageInsereeRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int compterAuMemeEndroit(Long idVersion, int numeroPage) {
        return jdbc.sql("SELECT count(*) FROM page_inseree WHERE id_version = :idVersion AND numero_page = :page")
                .param("idVersion", idVersion).param("page", numeroPage)
                .query(Integer.class).single();
    }

    @Override
    public void decalerOrdres(Long idVersion, int numeroPage, int aPartirDe) {
        jdbc.sql("""
                        UPDATE page_inseree SET ordre = ordre + 1
                         WHERE id_version = :idVersion AND numero_page = :page AND ordre >= :aPartirDe
                        """)
                .param("idVersion", idVersion).param("page", numeroPage).param("aPartirDe", aPartirDe)
                .update();
    }

    @Override
    public PageQuiz enregistrer(PageQuiz page) {
        page.identifier(jdbc.sql("""
                        INSERT INTO page_inseree (numero_page, repere_texte, ordre, valide, id_version, id_question)
                        VALUES (:page, :repere, :ordre, :valide, :idVersion, :idQuestion)
                        RETURNING id_page
                        """)
                .param("page", page.numeroPage())
                .param("repere", page.repereTexte())
                .param("ordre", page.ordre())
                .param("valide", page.valide())
                .param("idVersion", page.idVersion())
                .param("idQuestion", page.idQuestion())
                .query(Long.class).single());
        return page;
    }

    @Override
    public List<PageInseree> listerParVersion(Long idVersion) {
        return jdbc.sql("""
                        SELECT id_page, numero_page, ordre, id_question FROM page_inseree
                         WHERE id_version = :idVersion ORDER BY numero_page, ordre
                        """)
                .param("idVersion", idVersion)
                .query((l, i) -> new PageInseree(l.getLong("id_page"), l.getInt("numero_page"), l.getInt("ordre"),
                        l.getObject("id_question") != null ? TypePage.QUIZ : TypePage.CLASSEMENT))
                .list();
    }
}
