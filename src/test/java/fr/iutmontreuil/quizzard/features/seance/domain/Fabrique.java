package fr.iutmontreuil.quizzard.features.seance.domain;

import java.time.LocalDateTime;
import java.util.List;

/** Données de test du domaine de la séance. */
final class Fabrique {

    static final LocalDateTime MAINTENANT = LocalDateTime.of(2026, 11, 24, 14, 0, 0);
    static final Long ID_PARTICIPANT = 7L;

    private Fabrique() {
    }

    /** QCM 1 : propositions 41 (juste), 42, 43. */
    static QuestionQcm qcmUneBonne(Bareme bareme) {
        return new QuestionQcm(1L, "Quel protocole sert à l'annuaire ?", 20, bareme, List.of(
                new Proposition(41L, "LDAP", true, 0),
                new Proposition(42L, "SMTP", false, 1),
                new Proposition(43L, "FTP", false, 2)));
    }

    /** QCM 2 : propositions 51, 52, 53 justes ; 54 fausse. */
    static QuestionQcm qcmTroisBonnes(Bareme bareme) {
        return new QuestionQcm(2L, "Protocoles de la couche application ?", 20, bareme, List.of(
                new Proposition(51L, "HTTP", true, 0),
                new Proposition(52L, "DNS", true, 1),
                new Proposition(53L, "SMTP", true, 2),
                new Proposition(54L, "IP", false, 3)));
    }

    /** Relier 3 : paires 61 à 64. */
    static QuestionPointsARelier relierQuatre(Bareme bareme) {
        return new QuestionPointsARelier(3L, "Relie chaque port à son protocole", 20, bareme, List.of(
                new Paire(61L, "80", "HTTP"),
                new Paire(62L, "53", "DNS"),
                new Paire(63L, "25", "SMTP"),
                new Paire(64L, "22", "SSH")));
    }

    /** Libre 4 : accepte « ldap » (simple) ; le vérificateur de test compare à l'identique. */
    static QuestionReponseLibre libre(Bareme bareme) {
        return new QuestionReponseLibre(4L, "Quel protocole sert à l'annuaire ?", 20, bareme,
                List.of(new Motif("ldap", ModeMotif.SIMPLE)),
                (motif, texte) -> motif.motif().equals(texte));
    }

    static ContenuReponse cochees(Long... ids) {
        return new ContenuReponse(List.of(ids), null, null);
    }

    static ContenuReponse liens(Lien... liens) {
        return new ContenuReponse(null, List.of(liens), null);
    }

    static ContenuReponse texte(String texte) {
        return new ContenuReponse(null, null, texte);
    }

    static Reponse repondre(Question question, ContenuReponse contenu) {
        Reponse reponse = Reponse.creer(question, contenu, ID_PARTICIPANT, MAINTENANT, 0);
        reponse.valider(question);
        return reponse;
    }
}
