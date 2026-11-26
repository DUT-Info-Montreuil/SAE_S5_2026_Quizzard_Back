package fr.iutmontreuil.quizzard.features.seance.application.port;

/** Annuaire LDAP de l'IUT, en lecture seule : les étudiants d'un quiz noté s'y authentifient (UC15). */
public interface AnnuaireLdap {

    /**
     * Vrai si l'annuaire répond dans le délai imparti. Une erreur de connexion renvoie faux : le cas d'usage décide
     * alors s'il exige l'autorisation de l'enseignant.
     */
    boolean estDisponible();
}
