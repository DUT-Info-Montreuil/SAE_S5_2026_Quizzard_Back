package fr.iutmontreuil.quizzard.features.seance.infrastructure.persistance;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.iutmontreuil.quizzard.features.seance.application.port.ReponseRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.DejaReponduException;
import fr.iutmontreuil.quizzard.features.seance.domain.Lien;
import fr.iutmontreuil.quizzard.features.seance.domain.Reponse;
import fr.iutmontreuil.quizzard.features.seance.domain.ReponseLibre;
import fr.iutmontreuil.quizzard.features.seance.domain.ReponseQcm;
import fr.iutmontreuil.quizzard.features.seance.domain.ReponseRelier;

/**
 * Réponses et leur contenu (reponse, reponse_choix, reponse_lien). Les écritures JDBC partent tout de suite :
 * une erreur de base est levée dans le cas d'usage, qui annule toute la transaction.
 */
@Repository
class PostgresReponseRepository implements ReponseRepository {

    private final JdbcClient jdbc;

    PostgresReponseRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean existePour(Long idParticipant, Long idQuestion) {
        return jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM reponse
                                        WHERE id_participant = :idParticipant AND id_question = :idQuestion)
                        """)
                .param("idParticipant", idParticipant)
                .param("idQuestion", idQuestion)
                .query(Boolean.class)
                .single();
    }

    /** La contrainte uq_reponse_participant_question est le dernier rempart contre deux envois simultanés. */
    @Override
    public Reponse enregistrer(Reponse reponse) {
        Long idReponse;
        try {
            idReponse = jdbc.sql("""
                            INSERT INTO reponse (horodatage_serveur, temps_reponse_ms, texte_libre, points_calcules,
                                                 points_finaux, statut_correction, id_participant, id_question)
                            VALUES (:horodatage, :tempsMs, :texte, :pointsCalcules, :pointsFinaux, :statut,
                                    :idParticipant, :idQuestion)
                            RETURNING id_reponse
                            """)
                    .param("horodatage", reponse.horodatageServeur())
                    .param("tempsMs", Math.toIntExact(reponse.tempsReponseMs()))
                    .param("texte", reponse instanceof ReponseLibre libre ? libre.texteLibre() : null)
                    .param("pointsCalcules", reponse.pointsCalcules())
                    .param("pointsFinaux", reponse.pointsFinaux())
                    .param("statut", reponse.statutCorrection().name())
                    .param("idParticipant", reponse.idParticipant())
                    .param("idQuestion", reponse.idQuestion())
                    .query(Long.class)
                    .single();
        } catch (DuplicateKeyException e) {
            throw new DejaReponduException();
        }
        reponse.identifier(idReponse);
        switch (reponse) {
            case ReponseQcm qcm -> qcm.choix().forEach(idProposition -> jdbc.sql(
                            "INSERT INTO reponse_choix (id_reponse, id_proposition) VALUES (:idReponse, :id)")
                    .param("idReponse", idReponse).param("id", idProposition).update());
            case ReponseRelier relier -> relier.liens().forEach(lien -> enregistrerLien(idReponse, lien));
            case ReponseLibre _ -> {
                // Le texte est déjà dans la ligne reponse.
            }
        }
        return reponse;
    }

    private void enregistrerLien(Long idReponse, Lien lien) {
        jdbc.sql("""
                        INSERT INTO reponse_lien (id_reponse, id_paire_gauche, id_paire_droite)
                        VALUES (:idReponse, :gauche, :droite)
                        """)
                .param("idReponse", idReponse)
                .param("gauche", lien.gauche())
                .param("droite", lien.droite())
                .update();
    }
}
