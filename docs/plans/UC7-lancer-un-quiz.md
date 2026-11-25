# Plan d'implémentation – UC7 « Lancer un quiz en séance » (back)

Périmètre : **back-end Spring Boot uniquement** (écran E3, élément 11 « Lancer la présentation », puis écran E5 « Quiz
en cours »). Spécification : dossier de conception, §4.3 (définition, cas d'erreur, diagrammes), §5.1.3 (services API)
et §5.2.3 (services fonctionnels). Branche : `feature/uc7-lancer-un-quiz-en-seance`, partie de `develop`.

Dans le périmètre : lancement (séance, code d'accès, lien, QR code, canal temps réel), puis déroulement (question
suivante, décompte côté serveur, clôture automatique ou manuelle, écrans intermédiaires, passage automatique, fin de
séance, relecture de l'état).

Hors périmètre : entrée des étudiants (UC8, UC15), enregistrement et notation des réponses (UC9), écran de classement
(UC10 à UC12), configuration du déroulé et du barème (UC13, UC14), connexion de l'enseignant (UC1), images des
questions (UC4).

## 1. État de départ

- `develop` : le socle seul. La feature `seance` est vide.
- UC9 (`feature/uc9-repondre-question`), UC3 (`feature/uc3-inserer-page-quiz`) et UC8
  (`feature/uc8-rejoindre-un-quiz`) ne sont pas fusionnées. L'UC7 ne part d'aucune d'elles.
- Le schéma `V1` a déjà `session_quiz` (états `PREPAREE`, `EN_COURS`, `CLOTUREE`).
- Aucune route ne crée encore de cours, de quiz ni de question : les tests et la démo les écrivent en SQL.

## 2. Décisions prises

| # | Décision |
|---|---|
| D1 | **Le dossier de conception fait foi** quand `docs/API.md` diffère (`ARCHITECTURE.md`) : routes `…/questions/suivante` et `…/questions/courante/cloture`, corps du lancement `{autoriserIdentiteNonVerifiee?}`. Le QR code est dans le JSON (PNG en base64), sans route `…/qrcode`. |
| D2 | **Fichiers communs copiés à l'identique** depuis l'UC9 (erreurs avec `code`, `HorlogeConfig`, `WebSocketConfig`, `SecurityConfig`, `EtatSession`, `TypeQuestion`, deux fichiers de test) et depuis l'UC3 (`EnseignantConnecte`). |
| D3 | **Classes propres à l'UC7 sous d'autres noms** que celles des branches non fusionnées : `SeanceAnimee` (dossier : `Session`), `SeanceAnimeeRepository`, `QuizDeSeanceRepository`, `DiffuseurSeance` (dossier : `DiffuseurTempsReel`), `GenerateurJetonSession` (dossier : `GenerateurJeton`). Feature `seance` (dossier : `session`). |
| D4 | État initial `PREPAREE` (le dossier écrit `EN_ATTENTE`, que la contrainte SQL refuse). |
| D5 | Version présentée : la **dernière version** du cours. Questions de la séance : ses pages de quiz, triées par page du PDF puis par ordre ; `position_courante` est le rang de la page (1 pour la première), comme le lit l'UC9. |
| D6 | **Une seule séance ouverte par enseignant** : la ligne de l'enseignant est verrouillée pendant le lancement, pour que deux lancements simultanés ne passent pas tous les deux. |
| D7 | `GET /api/sessions/ouverte` (ajout au dossier) : après le `409` « séance déjà ouverte », le front retrouve la séance à reprendre ou à terminer. |
| D8 | Migration `V3` (la `V2` est prise par l'UC3) : `session_quiz.identite_non_verifiee_autorisee`. |
| D9 | Clôture automatique par une tâche du planificateur Spring, programmée à l'échéance plus 250 ms ; les échéances en cours sont reprogrammées au redémarrage. |
| D10 | Annuaire : adaptateur LDAPS en lecture seule (`LDAP_URL`). Sans adresse configurée, l'annuaire est « indisponible ». |
| D11 | Messages temps réel `…/etat` et `…/question` identiques à ceux de l'UC9 (le front les lit déjà). |
| D12 | `/user/queue/resultat` porte aussi `rang`, `nombreParticipants` et `evolution`, exigés par le front de l'UC9. |
| D13 | La réponse du lancement ajoute `codeAcces` aux champs du dossier : le front en a besoin pour s'abonner aux canaux. |

## 3. Contrat

Routes réservées à l'enseignant connecté et propriétaire (session de connexion, jeton CSRF sur les `POST`).

| Méthode | Route | Succès |
|---|---|---|
| `POST` | `/api/quiz/{idQuiz}/sessions` | `201`, `Location: /api/sessions/{idSession}`, séance créée |
| `GET` | `/api/sessions/ouverte` | `200`, état de la séance ouverte |
| `GET` | `/api/sessions/{idSession}` | `200`, état de la séance |
| `POST` | `/api/sessions/{idSession}/questions/suivante` | `200`, état (séance close après la dernière question) |
| `POST` | `/api/sessions/{idSession}/questions/courante/cloture` | `200`, état avec la correction |
| `POST` | `/api/sessions/{idSession}/cloture` | `200`, état `CLOTUREE` |

| Canal STOMP | Événement du dossier | Destinataires |
|---|---|---|
| `/topic/sessions/{codeAcces}/etat` | ouverture, clôture, `session_terminee` | Tous |
| `/topic/sessions/{codeAcces}/question` | `question_demarree` (sans réponses attendues) | Tous |
| `/topic/sessions/{codeAcces}/correction` | `question_close` (bonne réponse, répartition) | Tous |
| `/user/queue/resultat` | `question_close` (points de l'étudiant) | Un étudiant |
| `/topic/sessions/{codeAcces}/ecran` | `ecran_intermediaire` | Écran projeté |

`participant_connecte` (`…/connectes`) est envoyé par l'UC8, `reponse_recue` (`…/repartition`) par l'UC9.

## 4. Tableau des erreurs

| Cas | Statut | `code` | Message |
|---|---|---|---|
| Pas connecté | `401` | — | « Vous devez être connecté. » |
| Quiz inexistant ou archivé | `404` | — | « Ce quiz n'existe pas ou a été supprimé. » |
| Quiz d'un autre enseignant | `403` | — | « Action non autorisée. » |
| Quiz sans question ou question incomplète | `422` | `QUIZ_INCOMPLET` | « Le quiz contient une question incomplète (page …). Complétez-la avant de lancer. » |
| Séance déjà ouverte | `409` | `SESSION_DEJA_OUVERTE` | « Une session est déjà ouverte pour le cours « … ». La reprendre ou la terminer ? » |
| Quiz noté, annuaire indisponible, sans autorisation | `409` | `ANNUAIRE_INDISPONIBLE` | « L'annuaire de l'IUT est indisponible. Autoriser l'entrée en identité non vérifiée ? » |
| QR code, code d'accès, base ou canal temps réel en panne | `500` | — | Message générique ; aucune séance créée |
| Séance inexistante | `404` | `SEANCE_INTROUVABLE` | « Cette séance n'existe pas ou a été supprimée. » |
| Séance d'un autre enseignant | `403` | — | « Action non autorisée. » |
| Séance terminée | `409` | `SEANCE_TERMINEE` | « Cette séance est terminée. » |
| « Suivant » pendant une question ouverte | `409` | `QUESTION_DEJA_OUVERTE` | « Une question est déjà ouverte. Clôturez-la avant de passer à la suivante. » |
| Clôture sans question ouverte | `409` | `AUCUNE_QUESTION_OUVERTE` | « Aucune question n'est ouverte. » |

## 5. Machine à états de la séance

```text
(lancement) ──▶ PREPAREE (position 0)
PREPAREE ──Suivant──▶ EN_COURS, question 1 ouverte (échéance = maintenant + durée)
question ouverte ──échéance ou clôture manuelle──▶ question close (échéance vide) ──▶ écrans intermédiaires
question close ──Suivant ou passage automatique──▶ question suivante ouverte
question close (dernière) ──Suivant ou passage automatique──▶ CLOTUREE
PREPAREE | EN_COURS ──Clore le quiz──▶ CLOTUREE (la question ouverte est d'abord close)
CLOTUREE ──▶ plus aucune action
```

## 6. Étapes

- [x] **B0 Préparation** : branche depuis `develop`, fichiers communs copiés, plan enregistré.
- [x] **B1 Socle** : migration `V3`, dépendance `com.google.zxing:core` 3.5.3 (accord du 25/11/2026).
- [x] **B2 Domaine `seance`** : `SeanceAnimee`, `Quiz`, `QuestionDeSeance` (complétude), `ClassementDeSeance`,
  exceptions.
- [x] **B3 Cas d'usage et ports** : `LancerQuiz`, `DemarrerQuestion`, `CloreQuestion`, `CloturerSeance`,
  `ConsulterSeance`, `ReprendreEcheances`.
- [x] **B4 Adaptateurs** : PostgreSQL (`session_quiz` en JPA, lectures du quiz en SQL), code d'accès, QR code,
  annuaire LDAPS.
- [x] **B5 Web** : `SessionController` (lancement) et `DeroulementController`.
- [x] **B6 Temps réel** : `StompDiffuseurSeance` et `MinuterieSpring` (clôture, écrans, passage automatique, reprise
  au redémarrage).
- [x] **B7 Démo** : profil `demo-uc7`.
- [x] **B8 Tests et vérification** : `./mvnw verify` sur la branche (104 tests), puis fusion simulée de l'UC9, l'UC8,
  l'UC3 et l'UC7 sur un clone jetable (334 tests, dont un parcours lancement → entrée → réponse → résultat écrit
  pour la simulation, non gardé).

### Essayer l'UC7 (profil `demo-uc7`)

Le profil crée l'enseignant, le cours et le quiz `910000` (QCM, points à relier, réponse libre). À lancer avec
`SECURITE_OUVERTE=true` dans `.env`, jamais sur le serveur, puis `POST /api/quiz/910000/sessions`.

```bash
SECURITE_OUVERTE=true ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo-uc7
```

La base de développement garde les migrations déjà appliquées : après un passage par la branche de l'UC3 (`V2`),
Flyway refuse de démarrer sur cette branche, et inversement. Repartir d'une base vierge : `docker compose down -v`
(efface les données locales).

### Fusion avec les autres branches

L'UC7 n'ajoute aucun conflit. Les seuls conflits de la fusion simulée existent déjà entre l'UC3 et l'UC9
(`SecurityConfig`, `GestionnaireErreurs`, `TODO.md`) : garder la version de l'UC9 pour les deux fichiers Java.

## 7. Questions ouvertes

### Pour l'équipe back

| # | Sujet | Proposition |
|---|---|---|
| O1 | UC9 lit la séance sans verrou : une réponse en cours d'enregistrement pendant une clôture manuelle peut manquer à la correction envoyée (quelques millisecondes ; son score est bien compté) | Lire la séance avec `FOR SHARE` dans `RepondreQuestion` |
| O2 | UC3 ne bloque l'insertion d'une page que pour une séance `EN_COURS` : entre le lancement et la première question, une insertion décale les positions | Bloquer dès que `etat <> 'CLOTUREE'` |
| O3 | Après fusion, le profil `demo` de l'UC9 double l'UC7 | Supprimer `DemoSeanceController` quand les quiz de démonstration passent par l'UC7 |
| O4 | Un connecté qui n'est pas enseignant reçoit `401` (`EnseignantConnecte`), le dossier prévoit `403` | À traiter avec l'UC1 (rôles) |
| O5 | La connexion STOMP de l'enseignant et le contrôle de ses abonnements | UC1 ; en attendant, `SECURITE_OUVERTE=true` après fusion avec l'UC9 |

### Pour l'équipe front

| # | Sujet |
|---|---|
| F1 | Les routes suivent le dossier (`…/questions/suivante`, `…/questions/courante/cloture`) ; l'ancienne liste (`question-suivante`, `cloture-question`, `qrcode`) est à remplacer dans le `docs/API.md` du front |
| F2 | Forme de `…/correction` et champs `repondu` et `bonneReponse` de `/user/queue/resultat` à confirmer (`docs/API.md` §3.4) |

### À valider avec le client

- Passage automatique sans délai réglé : 5 s.
- « Clore le quiz » pendant une question la clôt et publie sa correction.
- Les pages d'une nouvelle version pas encore validées (`valide = false`) sont présentées.
- À score égal, un étudiant sans réponse (temps cumulé nul) passe devant un étudiant qui a répondu faux : conséquence
  de la règle de `DESIGN.md` §7.

### Pas encore fait

- Images des questions (aucune route ne les sert).
- `POST /api/sessions/{idSession}/identites-non-verifiees` (UC15).
- Annuaire : seuls les cas d'indisponibilité sont testés, aucun serveur LDAP n'étant disponible.
- Test de charge à 300 participants.
- Une contrainte de base violée répond `409` (règle commune de `GestionnaireErreurs`), pas le `500` du dossier ; la
  seule possible au lancement serait un code d'accès tiré deux fois.

## 8. Critères d'acceptation

- [x] Le lancement crée la séance, le code d'accès, le lien et le QR code, ou ne crée rien.
- [x] Chaque cas d'erreur du §4 renvoie son statut et son message (pannes simulées pour le QR code et le canal temps
  réel, pas pour la base).
- [x] Aucune bonne réponse ne sort du serveur avant la clôture de la question.
- [x] Une question se ferme seule à l'échéance, sur l'horloge du serveur.
- [x] Aucune donnée personnelle, aucun code d'accès dans les logs métier.
- [x] `./mvnw verify` passe, sur la branche et après fusion simulée.
