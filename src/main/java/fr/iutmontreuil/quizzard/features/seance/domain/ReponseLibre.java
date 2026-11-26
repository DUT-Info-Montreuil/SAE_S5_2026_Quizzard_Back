package fr.iutmontreuil.quizzard.features.seance.domain;

import java.time.LocalDateTime;

/** Réponse libre : le texte saisi, conservé tel quel (la normalisation sert seulement à comparer). */
public final class ReponseLibre extends Reponse {

    private final String texteLibre;

    ReponseLibre(Long idParticipant, Long idQuestion, LocalDateTime horodatage, long tempsMs, String texteLibre) {
        super(idParticipant, idQuestion, horodatage, tempsMs);
        this.texteLibre = texteLibre;
    }

    public String texteLibre() {
        return texteLibre;
    }

    /** Texte non vide une fois les espaces retirés, de 1000 caractères au plus. */
    @Override
    public void valider(Question question) {
        if (!(question instanceof QuestionReponseLibre)
                || texteLibre.isBlank()
                || texteLibre.length() > TexteReponse.LONGUEUR_MAX) {
            throw new ReponseInvalideException();
        }
    }
}
