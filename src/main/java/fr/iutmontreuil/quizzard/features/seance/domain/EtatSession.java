package fr.iutmontreuil.quizzard.features.seance.domain;

/** État d'une séance : PREPAREE (lancée, avant la première question), EN_COURS, CLOTUREE (DESIGN.md §4). */
public enum EtatSession {
    PREPAREE,
    EN_COURS,
    CLOTUREE
}
