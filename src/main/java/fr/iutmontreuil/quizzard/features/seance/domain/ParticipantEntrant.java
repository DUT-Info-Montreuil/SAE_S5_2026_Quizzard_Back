package fr.iutmontreuil.quizzard.features.seance.domain;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Étudiant présent dans une séance. Un participant n'existe que dans une séance.
 * <p>
 * Cette vue couvre l'entrée sans compte d'un quiz non noté : aucune identité, {@code identiteVerifiee} faux.
 * L'identité (identifiant, nom, prénom) s'ajoutera avec le cas « S'authentifier à un quiz noté ».
 * <p>
 * Le jeton du participant n'est jamais conservé : seule son empreinte SHA-256 l'est. {@link #toString()} ne
 * montre ni le pseudonyme ni l'empreinte.
 */
public final class ParticipantEntrant {

    private static final Pattern EMPREINTE = Pattern.compile("[0-9a-f]{64}");

    private final Long idParticipant;
    private final Long idSession;
    private final Pseudonyme pseudonyme;
    private final String empreinteJeton;
    private final int scoreTotal;
    private final boolean identiteVerifiee;
    private final LocalDateTime dateEntree;
    private final LocalDateTime derniereActivite;

    private ParticipantEntrant(Long idParticipant, Long idSession, Pseudonyme pseudonyme, String empreinteJeton,
                        int scoreTotal, boolean identiteVerifiee, LocalDateTime dateEntree,
                        LocalDateTime derniereActivite) {
        this.idParticipant = idParticipant;
        this.idSession = idSession;
        this.pseudonyme = pseudonyme;
        this.empreinteJeton = empreinteJeton;
        this.scoreTotal = scoreTotal;
        this.identiteVerifiee = identiteVerifiee;
        this.dateEntree = dateEntree;
        this.derniereActivite = derniereActivite;
    }

    /**
     * Fabrique le participant qui entre dans la séance, avec un score à 0. Il n'est pas encore enregistré
     * (pas d'identifiant) ; date d'entrée et dernière activité valent {@code maintenant}.
     *
     * @param empreinteJeton SHA-256 du jeton en hexadécimal minuscule (64 caractères)
     * @throws IllegalArgumentException si l'empreinte n'a pas 64 caractères hexadécimaux : erreur de
     *                                  programmation, car elle vient du générateur de jetons
     */
    public static ParticipantEntrant rejoindre(Long idSession, Pseudonyme pseudonyme, String empreinteJeton,
                                        LocalDateTime maintenant) {
        if (empreinteJeton == null || !EMPREINTE.matcher(empreinteJeton).matches()) {
            throw new IllegalArgumentException("L'empreinte du jeton doit faire 64 caractères hexadécimaux minuscules.");
        }
        return new ParticipantEntrant(null, idSession, pseudonyme, empreinteJeton, 0, false, maintenant, maintenant);
    }

    /** Reconstitue un participant déjà enregistré (adaptateur de persistance). */
    public static ParticipantEntrant reconstituer(Long idParticipant, Long idSession, Pseudonyme pseudonyme,
                                           String empreinteJeton, int scoreTotal, boolean identiteVerifiee,
                                           LocalDateTime dateEntree, LocalDateTime derniereActivite) {
        return new ParticipantEntrant(idParticipant, idSession, pseudonyme, empreinteJeton, scoreTotal, identiteVerifiee,
                dateEntree, derniereActivite);
    }

    /** Le même participant, avec l'identifiant attribué à l'enregistrement. */
    public ParticipantEntrant avecIdentifiant(Long id) {
        return new ParticipantEntrant(id, idSession, pseudonyme, empreinteJeton, scoreTotal, identiteVerifiee, dateEntree,
                derniereActivite);
    }

    public Long idParticipant() {
        return idParticipant;
    }

    public Long idSession() {
        return idSession;
    }

    public Pseudonyme pseudonyme() {
        return pseudonyme;
    }

    public String empreinteJeton() {
        return empreinteJeton;
    }

    public int scoreTotal() {
        return scoreTotal;
    }

    public boolean identiteVerifiee() {
        return identiteVerifiee;
    }

    public LocalDateTime dateEntree() {
        return dateEntree;
    }

    public LocalDateTime derniereActivite() {
        return derniereActivite;
    }

    @Override
    public String toString() {
        return "ParticipantEntrant[idParticipant=" + idParticipant + ", idSession=" + idSession + "]";
    }
}
