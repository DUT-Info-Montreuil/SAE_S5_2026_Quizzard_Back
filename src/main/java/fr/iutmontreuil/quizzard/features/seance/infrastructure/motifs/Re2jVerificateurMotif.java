package fr.iutmontreuil.quizzard.features.seance.infrastructure.motifs;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;

import fr.iutmontreuil.quizzard.features.seance.domain.ModeMotif;
import fr.iutmontreuil.quizzard.features.seance.domain.Motif;
import fr.iutmontreuil.quizzard.features.seance.domain.TexteReponse;
import fr.iutmontreuil.quizzard.features.seance.domain.VerificateurMotif;

/**
 * Motifs appliqués avec RE2/J, moteur d'expressions régulières à temps linéaire : un motif pathologique saisi par
 * un enseignant ne peut pas bloquer le serveur (DESIGN.md §5). Le motif s'applique à toute la chaîne.
 * <ul>
 *   <li>SIMPLE : égalité avec le texte normalisé, le motif étant normalisé de la même façon ; {@code *} remplace
 *   n'importe quelle suite de caractères (à valider) ;</li>
 *   <li>REGEX : expression régulière appliquée au texte normalisé.</li>
 * </ul>
 * Un motif trop long ou invalide n'accepte rien (il aurait dû être refusé à l'enregistrement).
 */
@Component
class Re2jVerificateurMotif implements VerificateurMotif {

    @Override
    public boolean accepte(Motif motif, String texteNormalise) {
        if (motif.motif() == null || motif.motif().length() > Motif.LONGUEUR_MAX) {
            return false;
        }
        try {
            return compiler(motif).matcher(texteNormalise).matches();
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    private static Pattern compiler(Motif motif) {
        if (motif.mode() == ModeMotif.REGEX) {
            return Pattern.compile(motif.motif());
        }
        String expression = Arrays.stream(TexteReponse.normaliser(motif.motif()).split("\\*", -1))
                .map(Pattern::quote)
                .collect(Collectors.joining(".*"));
        return Pattern.compile(expression);
    }
}
