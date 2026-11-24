package fr.iutmontreuil.quizzard.features.seance.infrastructure.persistance;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import fr.iutmontreuil.quizzard.features.seance.application.port.ParticipantRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.Participant;

/** Participants : ni pseudonyme ni identité ne sont lus, le cas d'usage n'en a pas besoin. */
@Repository
class PostgresParticipantRepository implements ParticipantRepository {

    private final JdbcClient jdbc;

    PostgresParticipantRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Participant> trouverParEmpreinteJeton(String empreinte) {
        return jdbc.sql("""
                        SELECT id_participant, id_session, score_total
                          FROM participant
                         WHERE jeton_reconnexion = :empreinte
                        """)
                .param("empreinte", empreinte)
                .query((ligne, i) -> new Participant(ligne.getLong("id_participant"), ligne.getLong("id_session"),
                        ligne.getInt("score_total")))
                .optional();
    }

    /** Une seule requête : deux réponses simultanées du même participant ne peuvent pas perdre de points. */
    @Override
    public void ajouterPoints(Long idParticipant, int points, LocalDateTime maintenant) {
        jdbc.sql("""
                        UPDATE participant
                           SET score_total = score_total + :points, derniere_activite = :maintenant
                         WHERE id_participant = :idParticipant
                        """)
                .param("points", points)
                .param("maintenant", maintenant)
                .param("idParticipant", idParticipant)
                .update();
    }
}
