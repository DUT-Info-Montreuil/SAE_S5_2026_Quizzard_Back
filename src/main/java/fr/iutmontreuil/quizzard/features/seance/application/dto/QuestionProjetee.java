package fr.iutmontreuil.quizzard.features.seance.application.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import fr.iutmontreuil.quizzard.features.seance.domain.PaireDeSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.QuestionDeSeance;
import fr.iutmontreuil.quizzard.features.seance.domain.TypeQuestion;

/**
 * Question telle qu'elle est projetée et envoyée aux étudiants : sans aucune réponse attendue (ni
 * {@code estCorrecte}, ni paires, ni motifs). Pour les points à relier, les deux colonnes sont mélangées séparément,
 * avec la même règle que la lecture d'état de l'UC9 : même ordre pour tous et à chaque rechargement.
 *
 * @param echeance       échéance sur l'horloge du serveur, ou {@code null} si la question est close
 * @param propositions   propositions d'un QCM, dans leur rang d'affichage (vide sinon)
 * @param choixMultiple  vrai si plusieurs propositions sont correctes (QCM seulement)
 * @param elementsGauche éléments de gauche (points à relier), désignés par l'identifiant de leur paire
 * @param elementsDroite éléments de droite (points à relier), désignés par l'identifiant de leur paire
 */
public record QuestionProjetee(
        Long idQuestion,
        int position,
        TypeQuestion type,
        String enonce,
        int dureeSec,
        LocalDateTime echeance,
        List<Element> propositions,
        boolean choixMultiple,
        List<Element> elementsGauche,
        List<Element> elementsDroite) {

    /** Élément affichable : un identifiant et un libellé. */
    public record Element(Long id, String libelle) {
    }

    public static QuestionProjetee de(QuestionDeSeance question, int position, LocalDateTime echeance) {
        List<Element> propositions = List.of();
        boolean choixMultiple = false;
        List<Element> gauche = List.of();
        List<Element> droite = List.of();
        switch (question.type()) {
            case QCM -> {
                propositions = question.propositions().stream()
                        .map(p -> new Element(p.idProposition(), p.libelle()))
                        .toList();
                choixMultiple = question.choixMultiple();
            }
            case POINTS_A_RELIER -> {
                List<PaireDeSeance> paires = question.paires().stream()
                        .sorted(Comparator.comparing(PaireDeSeance::idPaire))
                        .toList();
                gauche = melanger(paires.stream().map(p -> new Element(p.idPaire(), p.elementGauche())).toList(),
                        question.idQuestion());
                droite = melanger(paires.stream().map(p -> new Element(p.idPaire(), p.elementDroit())).toList(),
                        ~question.idQuestion());
            }
            case REPONSE_LIBRE -> {
                // Ni motif ni réponse attendue : l'énoncé suffit.
            }
        }
        return new QuestionProjetee(question.idQuestion(), position, question.type(), question.enonce(),
                question.dureeSec(), echeance, propositions, choixMultiple, gauche, droite);
    }

    /** Mélange stable, dont la graine est tirée de la question. */
    private static List<Element> melanger(List<Element> elements, long graine) {
        List<Element> copie = new ArrayList<>(elements);
        Collections.shuffle(copie, new Random(graine));
        return List.copyOf(copie);
    }
}
