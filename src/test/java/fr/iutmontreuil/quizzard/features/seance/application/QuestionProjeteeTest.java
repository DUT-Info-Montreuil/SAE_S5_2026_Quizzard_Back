package fr.iutmontreuil.quizzard.features.seance.application;

import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.libre;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.qcm;
import static fr.iutmontreuil.quizzard.features.seance.domain.FabriqueLancement.relier;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import fr.iutmontreuil.quizzard.features.seance.application.dto.CorrectionQuestion;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QuestionProjetee;
import fr.iutmontreuil.quizzard.features.seance.application.dto.QuestionProjetee.Element;
import fr.iutmontreuil.quizzard.features.seance.application.dto.RepartitionReponses;

class QuestionProjeteeTest {

    @Test
    void un_qcm_projete_ne_contient_que_les_libelles_et_le_choix_multiple() {
        QuestionProjetee projetee = QuestionProjetee.de(qcm(7, 3), 1, null);

        assertThat(projetee.propositions()).containsExactly(new Element(70L, "LDAP"), new Element(71L, "SMTP"));
        assertThat(projetee.choixMultiple()).isFalse();
        assertThat(projetee.elementsGauche()).isEmpty();
    }

    @Test
    void les_deux_colonnes_a_relier_sont_melangees_comme_le_fait_la_lecture_d_etat_de_l_uc9() {
        QuestionProjetee projetee = QuestionProjetee.de(relier(42, 3), 1, null);

        // Règle de l'UC9 : paires par identifiant, mélangées avec l'identifiant de la question (gauche) et son
        // complément (droite).
        List<Element> gauche = new ArrayList<>(List.of(new Element(1L, "80"), new Element(2L, "53"),
                new Element(3L, "22"), new Element(4L, "25")));
        Collections.shuffle(gauche, new Random(42));
        List<Element> droite = new ArrayList<>(List.of(new Element(1L, "HTTP"), new Element(2L, "DNS"),
                new Element(3L, "SSH"), new Element(4L, "SMTP")));
        Collections.shuffle(droite, new Random(~42L));
        assertThat(projetee.elementsGauche()).containsExactlyElementsOf(gauche);
        assertThat(projetee.elementsDroite()).containsExactlyElementsOf(droite);
        assertThat(projetee.propositions()).isEmpty();
    }

    @Test
    void une_reponse_libre_ne_montre_aucun_motif() {
        QuestionProjetee projetee = QuestionProjetee.de(libre(5, 3), 1, null);

        assertThat(projetee.toString()).doesNotContain("https").doesNotContain("tls");
    }

    @Test
    void la_correction_montre_les_motifs_simples_mais_pas_les_expressions_regulieres() {
        CorrectionQuestion correction = CorrectionQuestion.de(libre(5, 3), 1,
                new RepartitionReponses(2, java.util.Map.of()));

        assertThat(correction.reponsesAcceptees()).containsExactly("https");
        assertThat(correction.total()).isEqualTo(2);
        assertThat(CorrectionQuestion.bonneReponseEnTexte(libre(5, 3))).containsExactly("https");
        assertThat(CorrectionQuestion.bonneReponseEnTexte(relier(1, 3))).first().isEqualTo("80 → HTTP");
    }
}
