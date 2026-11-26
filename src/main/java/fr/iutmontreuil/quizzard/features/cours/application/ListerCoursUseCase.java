package fr.iutmontreuil.quizzard.features.cours.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.iutmontreuil.quizzard.features.cours.application.dto.CoursConsulte;
import fr.iutmontreuil.quizzard.features.cours.application.port.ConsultationCoursRepository;

/** Liste « Mes cours » (écran E2), où apparaît un cours importé (UC2). Limitée aux cours de l'enseignant. */
@Service
public class ListerCoursUseCase {

    private final ConsultationCoursRepository cours;

    public ListerCoursUseCase(ConsultationCoursRepository cours) {
        this.cours = cours;
    }

    /** @return les cours non archivés de l'enseignant, le plus récemment modifié d'abord ; vide s'il n'en a pas */
    @Transactional(readOnly = true)
    public List<CoursConsulte> executer(Long idEnseignant) {
        return cours.listerParEnseignant(idEnseignant);
    }
}
