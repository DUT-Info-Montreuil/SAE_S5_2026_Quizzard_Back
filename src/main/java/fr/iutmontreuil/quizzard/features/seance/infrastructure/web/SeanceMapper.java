package fr.iutmontreuil.quizzard.features.seance.infrastructure.web;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.features.seance.application.dto.EtatSeanceEtudiant;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QuestionPourEtudiant;
import fr.iutmontreuil.quizzard.features.seance.application.dto.ReponseEnregistree;
import fr.iutmontreuil.quizzard.features.seance.domain.TypeQuestion;
import fr.iutmontreuil.quizzard.features.seance.infrastructure.web.EtatSeanceResponse.ElementResponse;
import fr.iutmontreuil.quizzard.features.seance.infrastructure.web.EtatSeanceResponse.PropositionResponse;
import fr.iutmontreuil.quizzard.features.seance.infrastructure.web.EtatSeanceResponse.QuestionResponse;

/**
 * Conversion des résultats des cas d'usage en JSON. Les dates partent avec leur décalage horaire
 * ({@code 2026-11-24T15:05:20+01:00}) : le navigateur les lit sans ambiguïté de fuseau.
 */
@Component
class SeanceMapper {

    private final Clock horloge;

    SeanceMapper(Clock horloge) {
        this.horloge = horloge;
    }

    ReponseEnregistreeResponse versResponse(ReponseEnregistree r) {
        return new ReponseEnregistreeResponse(r.idQuestion(), avecDecalage(r.horodatageServeur()));
    }

    EtatSeanceResponse versResponse(EtatSeanceEtudiant etat) {
        return new EtatSeanceResponse(versResponse(etat.question()), etat.dejaRepondu(), etat.scoreTotal());
    }

    QuestionResponse versResponse(QuestionPourEtudiant q) {
        if (q == null) {
            return null;
        }
        boolean qcm = q.type() == TypeQuestion.QCM;
        boolean relier = q.type() == TypeQuestion.POINTS_A_RELIER;
        return new QuestionResponse(q.idQuestion(), q.position(), q.type().name(), q.enonce(), q.dureeSec(),
                avecDecalage(q.echeance()),
                qcm ? q.propositions().stream().map(c -> new PropositionResponse(c.id(), c.libelle())).toList() : null,
                qcm ? q.choixMultiple() : null,
                relier ? elements(q.elementsGauche()) : null,
                relier ? elements(q.elementsDroite()) : null);
    }

    private static List<ElementResponse> elements(List<QuestionPourEtudiant.Choix> choix) {
        return choix.stream().map(c -> new ElementResponse(c.id(), c.libelle())).toList();
    }

    OffsetDateTime avecDecalage(LocalDateTime date) {
        return date == null ? null : date.atZone(horloge.getZone()).toOffsetDateTime();
    }
}
