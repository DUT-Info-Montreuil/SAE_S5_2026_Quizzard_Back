package fr.iutmontreuil.quizzard.features.seance.domain;

/**
 * Conditions à remplir pour qu'un étudiant entre dans une séance (UC8, docs/DESIGN.md §4). Elles lisent
 * {@link SessionQuiz}, partagé avec les autres cas d'usage de la séance, sans le modifier.
 */
public final class ConditionsEntree {

    private ConditionsEntree() {
    }

    /**
     * Vérifie que la séance n'est pas close : une séance prête ({@code PREPAREE}) ou en cours ({@code EN_COURS})
     * accepte les étudiants.
     *
     * @throws SessionCloseException si la séance est close
     */
    public static void verifierSeanceOuverte(SessionQuiz session) {
        if (session.etat() == EtatSession.CLOTUREE) {
            throw new SessionCloseException();
        }
    }

    /**
     * Vérifie que la séance accepte une entrée sans compte : séance non close et quiz non noté.
     *
     * @throws SessionCloseException         si la séance est close
     * @throws ConnexionIutRequiseException si le quiz est noté
     */
    public static void verifierEntreeSansCompte(SessionQuiz session) {
        verifierSeanceOuverte(session);
        if (session.estNotee()) {
            throw new ConnexionIutRequiseException();
        }
    }
}
