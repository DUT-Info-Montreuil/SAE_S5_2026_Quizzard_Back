package fr.iutmontreuil.quizzard.features.seance.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Réponse d'un participant à une question (UC9). Une seule par participant et par question. Elle est horodatée par
 * le serveur ; aucun horodatage envoyé par le client n'est pris en compte.
 */
public abstract sealed class Reponse permits ReponseQcm, ReponseRelier, ReponseLibre {

    private Long idReponse;
    private final Long idParticipant;
    private final Long idQuestion;
    private final LocalDateTime horodatageServeur;
    private final long tempsReponseMs;
    private int pointsCalcules;
    private int pointsFinaux;
    private StatutCorrection statutCorrection = StatutCorrection.SANS_OBJET;

    protected Reponse(Long idParticipant, Long idQuestion, LocalDateTime horodatageServeur, long tempsReponseMs) {
        this.idParticipant = idParticipant;
        this.idQuestion = idQuestion;
        this.horodatageServeur = horodatageServeur;
        this.tempsReponseMs = tempsReponseMs;
    }

    /**
     * Fabrique la réponse du type de la question à partir du contenu reçu. Seul le champ du type de la question est
     * lu ; les autres sont ignorés.
     *
     * @throws ReponseInvalideException si le contenu attendu pour ce type est absent
     */
    public static Reponse creer(Question question, ContenuReponse contenu, Long idParticipant,
                                LocalDateTime maintenant, long tempsMs) {
        Long idQuestion = question.idQuestion();
        return switch (question) {
            case QuestionQcm _ -> new ReponseQcm(idParticipant, idQuestion, maintenant, tempsMs,
                    exigerListe(contenu.idPropositions()));
            case QuestionPointsARelier _ -> new ReponseRelier(idParticipant, idQuestion, maintenant, tempsMs,
                    exigerListe(contenu.liens()));
            case QuestionReponseLibre _ -> {
                if (contenu.texteLibre() == null) {
                    throw new ReponseInvalideException();
                }
                yield new ReponseLibre(idParticipant, idQuestion, maintenant, tempsMs, contenu.texteLibre());
            }
        };
    }

    /**
     * Vérifie que le contenu est cohérent avec la question : propositions ou paires de cette question, aucun élément
     * en double, texte non vide et de longueur correcte.
     *
     * @throws ReponseInvalideException si le contenu est incohérent
     */
    public abstract void valider(Question question);

    /** Fixe les points calculés à la réception ; les points finaux partent de la même valeur. */
    public void attribuerPoints(int points, StatutCorrection statut) {
        this.pointsCalcules = points;
        this.pointsFinaux = points;
        this.statutCorrection = statut;
    }

    /** Identifiant attribué par la base à l'enregistrement. */
    public void identifier(Long id) {
        this.idReponse = id;
    }

    private static <T> List<T> exigerListe(List<T> liste) {
        if (liste == null || liste.isEmpty() || liste.stream().anyMatch(Objects::isNull)) {
            throw new ReponseInvalideException();
        }
        return List.copyOf(liste);
    }

    public Long idReponse() {
        return idReponse;
    }

    public Long idParticipant() {
        return idParticipant;
    }

    public Long idQuestion() {
        return idQuestion;
    }

    public LocalDateTime horodatageServeur() {
        return horodatageServeur;
    }

    public long tempsReponseMs() {
        return tempsReponseMs;
    }

    public int pointsCalcules() {
        return pointsCalcules;
    }

    public int pointsFinaux() {
        return pointsFinaux;
    }

    public StatutCorrection statutCorrection() {
        return statutCorrection;
    }
}
