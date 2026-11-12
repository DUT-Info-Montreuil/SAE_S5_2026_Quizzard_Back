package fr.iutmontreuil.quizzard.shared.application.port;

import java.util.Map;

/**
 * Port de journalisation des événements métier, appelé par les cas d'usage (couche application).
 * Le cas d'usage le reçoit par son constructeur, comme un repository : il ne connaît ni la bibliothèque
 * de logs ni la base. L'implémentation vit en infrastructure (cours « Journaliser son API »).
 * <p>
 * Les champs ne contiennent jamais de donnée personnelle : des identifiants, des codes, des nombres.
 * Le nom de l'événement est en snake_case, au passé : {@code cours_importe}, {@code session_lancee}.
 */
public interface JournalEvenements {

    /** Événement métier réussi, écrit dans les logs (INFO). Exemple : {@code reponse_enregistree}. */
    void evenement(String nom, Map<String, Object> champs);

    /** Une règle métier a refusé l'opération, sans panne (WARN). Exemple : {@code import_refuse_doublon}. */
    void regleRefusee(String nom, Map<String, Object> champs);

    /**
     * Connexion ou action sensible, conservée 6 mois dans la table {@code journal_evenement} en plus des logs :
     * connexion, import, suppression, archivage, lancement de séance, correction, export (dossier §3.1.4).
     * Le nom fait au plus 30 caractères. Le champ {@code cible}, s'il est fourni, désigne l'objet concerné.
     */
    void actionSensible(String nom, Map<String, Object> champs);
}
