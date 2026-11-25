package fr.iutmontreuil.quizzard.features.cours.infrastructure.persistance;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.iutmontreuil.quizzard.features.cours.application.port.PageInsereeRepository;
import fr.iutmontreuil.quizzard.features.cours.domain.PageInseree;
import fr.iutmontreuil.quizzard.features.cours.domain.PageInseree.TypePage;
import fr.iutmontreuil.quizzard.features.cours.domain.PagePlacee;
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
                        SELECT p.id_page, p.numero_page, p.ordre, p.id_question, z.id_quiz, z.titre
                          FROM page_inseree p
                          LEFT JOIN question q ON q.id_question = p.id_question
                          LEFT JOIN quiz z ON z.id_quiz = q.id_quiz
                         WHERE p.id_version = :idVersion
                         ORDER BY p.numero_page, p.ordre
                        """)
                .param("idVersion", idVersion)
                .query((l, i) -> {
                    Long idQuestion = l.getObject("id_question", Long.class);
                    return new PageInseree(l.getLong("id_page"), l.getInt("numero_page"), l.getInt("ordre"),
                            idQuestion != null ? TypePage.QUIZ : TypePage.CLASSEMENT, idQuestion,
                            l.getObject("id_quiz", Long.class), l.getString("titre"));
                })
                .list();
    }

    @Override
    public Optional<PagePlacee> trouverParId(Long idPage) {
        return jdbc.sql("""
                        SELECT p.id_page, p.id_version, v.id_cours, p.numero_page, p.ordre, q.id_quiz
                          FROM page_inseree p
                          JOIN version_cours v ON v.id_version = p.id_version
                          LEFT JOIN question q ON q.id_question = p.id_question
                         WHERE p.id_page = :idPage
                        """)
                .param("idPage", idPage)
                .query((l, i) -> new PagePlacee(l.getLong("id_page"), l.getLong("id_version"), l.getLong("id_cours"),
                        l.getInt("numero_page"), l.getInt("ordre"), l.getObject("id_quiz", Long.class)))
                .optional();
    }

    @Override
    public int compterAuMemeEndroitSauf(Long idVersion, int numeroPage, Long idPageExclue) {
        return jdbc.sql("""
                        SELECT count(*) FROM page_inseree
                         WHERE id_version = :idVersion AND numero_page = :page AND id_page <> :exclue
                        """)
                .param("idVersion", idVersion).param("page", numeroPage).param("exclue", aucune(idPageExclue))
                .query(Integer.class).single();
    }

    @Override
    public void decalerOrdresSauf(Long idVersion, int numeroPage, int aPartirDe, Long idPageExclue) {
        jdbc.sql("""
                        UPDATE page_inseree SET ordre = ordre + 1
                         WHERE id_version = :idVersion AND numero_page = :page AND ordre >= :aPartirDe
                           AND id_page <> :exclue
                        """)
                .param("idVersion", idVersion).param("page", numeroPage).param("aPartirDe", aPartirDe)
                .param("exclue", aucune(idPageExclue))
                .update();
    }

    @Override
    public void placer(Long idPage, int numeroPage, int ordre) {
        jdbc.sql("UPDATE page_inseree SET numero_page = :page, ordre = :ordre WHERE id_page = :idPage")
                .param("page", numeroPage).param("ordre", ordre).param("idPage", idPage)
                .update();
    }

    @Override
    public void supprimer(Long idPage) {
        jdbc.sql("DELETE FROM page_inseree WHERE id_page = :idPage").param("idPage", idPage).update();
    }

    /** Une seule requête : les rangs deviennent 0, 1, 2… dans leur ordre actuel (contrainte de position différée). */
    @Override
    public void recaler(Long idVersion, int numeroPage, Long idPageExclue) {
        jdbc.sql("""
                        UPDATE page_inseree p SET ordre = r.rang - 1
                          FROM (SELECT id_page, row_number() OVER (ORDER BY ordre, id_page) AS rang
                                  FROM page_inseree
                                 WHERE id_version = :idVersion AND numero_page = :page AND id_page <> :exclue) r
                         WHERE p.id_page = r.id_page
                        """)
                .param("idVersion", idVersion).param("page", numeroPage).param("exclue", aucune(idPageExclue))
                .update();
    }

    /** Identifiant d'exclusion : 0 (aucune page n'a cet identifiant) quand il n'y a rien à exclure. */
    private static long aucune(Long idPageExclue) {
        return idPageExclue == null ? 0L : idPageExclue;
    }
}
