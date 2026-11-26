package fr.iutmontreuil.quizzard.features.seance.infrastructure.persistance;

import java.sql.SQLException;
import java.util.Set;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import fr.iutmontreuil.quizzard.features.seance.application.port.EntreeParticipantRepository;
import fr.iutmontreuil.quizzard.features.seance.domain.ParticipantEntrant;
import fr.iutmontreuil.quizzard.features.seance.domain.Pseudonyme;
import fr.iutmontreuil.quizzard.features.seance.domain.PseudonymeDejaPrisException;

/**
 * Adaptateur du port {@link EntreeParticipantRepository}. L'écriture est forcée ({@code saveAndFlush}) pour qu'une
 * erreur de base soit détectée dans le cas d'usage, donc avant le commit et avant toute diffusion.
 */
@Repository
class PostgresEntreeParticipantRepository implements EntreeParticipantRepository {

    /** Contrainte {@code UNIQUE (id_session, pseudonyme)} de {@code V1__schema_initial.sql}. */
    static final String CONTRAINTE_PSEUDONYME = "uq_participant_pseudonyme";

    private static final String SQLSTATE_UNICITE = "23505";

    private final ParticipantJpaRepository jpa;

    PostgresEntreeParticipantRepository(ParticipantJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existePseudonyme(Long idSession, Pseudonyme pseudonyme) {
        return jpa.existsByIdSessionAndPseudonyme(idSession, pseudonyme.valeur());
    }

    @Override
    public Set<String> pseudonymesDeLaSession(Long idSession) {
        return jpa.pseudonymesDeLaSession(idSession);
    }

    @Override
    public ParticipantEntrant enregistrer(ParticipantEntrant participant) {
        try {
            ParticipantEntity enregistre = jpa.saveAndFlush(versEntite(participant));
            return participant.avecIdentifiant(enregistre.idParticipant);
        } catch (DataIntegrityViolationException e) {
            if (violeUnicitePseudonyme(e)) {
                // Deux étudiants ont envoyé le même pseudonyme au même instant : le second est refusé, sans proposition.
                throw new PseudonymeDejaPrisException();
            }
            throw e;
        }
    }

    @Override
    public int compter(Long idSession) {
        return Math.toIntExact(jpa.countByIdSession(idSession));
    }

    private static ParticipantEntity versEntite(ParticipantEntrant participant) {
        ParticipantEntity entite = new ParticipantEntity();
        entite.idParticipant = participant.idParticipant();
        entite.idSession = participant.idSession();
        entite.pseudonyme = participant.pseudonyme().valeur();
        entite.jetonReconnexion = participant.empreinteJeton();
        entite.scoreTotal = participant.scoreTotal();
        entite.identiteVerifiee = participant.identiteVerifiee();
        entite.dateEntree = participant.dateEntree();
        entite.derniereActivite = participant.derniereActivite();
        return entite;
    }

    /**
     * Vrai si l'erreur vient de la contrainte d'unicité du pseudonyme. On ne lit que le nom de la contrainte et le
     * code SQLSTATE : le message de PostgreSQL recopie les valeurs en cause, donc le pseudonyme.
     */
    private static boolean violeUnicitePseudonyme(DataIntegrityViolationException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && CONTRAINTE_PSEUDONYME.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
            if (cause instanceof SQLException sql && SQLSTATE_UNICITE.equals(sql.getSQLState())
                    && sql.getMessage() != null && sql.getMessage().contains(CONTRAINTE_PSEUDONYME)) {
                return true;
            }
        }
        return false;
    }
}
