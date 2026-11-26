package fr.iutmontreuil.quizzard.features.seance.infrastructure.format;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import fr.iutmontreuil.quizzard.features.seance.application.dto.CorrectionQuestion;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QuestionProjetee;
import fr.iutmontreuil.quizzard.features.seance.application.dto.RepartitionReponses;
import fr.iutmontreuil.quizzard.features.seance.domain.TypeQuestion;

/**
 * Formes JSON communes aux routes REST et au canal temps réel. Les dates partent avec leur décalage horaire
 * ({@code 2026-11-24T15:05:20+01:00}) : le navigateur les lit sans ambiguïté de fuseau.
 */
@Component
public class FormatSeance {

    private final Clock horloge;

    public FormatSeance(Clock horloge) {
        this.horloge = horloge;
    }

    public OffsetDateTime date(LocalDateTime date) {
        return date == null ? null : date.atZone(horloge.getZone()).toOffsetDateTime();
    }

    public QuestionJson question(QuestionProjetee q) {
        if (q == null) {
            return null;
        }
        boolean qcm = q.type() == TypeQuestion.QCM;
        boolean relier = q.type() == TypeQuestion.POINTS_A_RELIER;
        return new QuestionJson(q.idQuestion(), q.position(), q.type().name(), q.enonce(), q.dureeSec(),
                date(q.echeance()),
                qcm ? q.propositions().stream()
                        .map(e -> new QuestionJson.Proposition(e.id(), e.libelle())).toList() : null,
                qcm ? q.choixMultiple() : null,
                relier ? elements(q.elementsGauche()) : null,
                relier ? elements(q.elementsDroite()) : null);
    }

    public CorrectionJson correction(CorrectionQuestion c) {
        if (c == null) {
            return null;
        }
        return new CorrectionJson(c.idQuestion(), c.position(), c.type().name(), c.bonnesPropositions(),
                c.paires().stream()
                        .map(a -> new CorrectionJson.Association(a.gauche(), a.droite(), a.libelleGauche(),
                                a.libelleDroite()))
                        .toList(),
                c.reponsesAcceptees(), c.total(),
                c.repartition().stream()
                        .map(p -> new RepartitionJson.ParProposition(p.idProposition(), p.nombre()))
                        .toList());
    }

    public RepartitionJson repartition(Long idQuestion, RepartitionReponses reponses) {
        List<RepartitionJson.ParProposition> propositions = reponses.parProposition().entrySet().stream()
                .map(e -> new RepartitionJson.ParProposition(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(RepartitionJson.ParProposition::idProposition))
                .toList();
        return new RepartitionJson(idQuestion, reponses.total(), propositions);
    }

    private static List<QuestionJson.Element> elements(List<QuestionProjetee.Element> elements) {
        return elements.stream().map(e -> new QuestionJson.Element(e.id(), e.libelle())).toList();
    }
}
