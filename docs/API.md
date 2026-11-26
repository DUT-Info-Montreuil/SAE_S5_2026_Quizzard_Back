# API du back-end

Contrat entre le back-end Spring Boot et le front React : routes REST et canal temps réel (WebSocket/STOMP).

> **État actuel** : seul le socle existe (`GET /actuator/health`, format d'erreur, sécurité, CORS). Les routes
> ci-dessous sont la **cible** proposée pour les features ; elles sont marquées par leur cas d'utilisation (UC) et
> leur priorité. Toute route ajoutée, renommée ou supprimée doit être reportée ici dans la même pull request, et
> annoncée à l'équipe front. La documentation Swagger/OpenAPI générée depuis le code (exigée à la soutenance 2)
> deviendra la référence détaillée des schémas ; ce fichier garde la vue d'ensemble.

---

## 1. Généralités

| Élément | Valeur |
|---|---|
| Préfixe | `/api` pour les routes métier ; `/api/public` pour les routes étudiants sans compte ; `/ws` pour le WebSocket |
| Format | JSON (`application/json; charset=UTF-8`), sauf import de fichier (`multipart/form-data`), PDF, images, QR code et CSV |
| Nommage JSON | `camelCase` en français (`idCours`, `dureeParDefautSec`) ; seul le format d'erreur garde `error` et `request_id` |
| Dates | ISO 8601 (`2026-11-12T14:05:00`), heure du serveur |
| Identifiants | Entiers (`id*`) ; les séances sont aussi désignées par leur `codeAcces` dans les routes publiques |
| Pagination | Inutile aux volumes prévus ; si besoin plus tard : `?page=0&taille=50` |
| Traçabilité | Chaque réponse porte l'en-tête `X-Request-Id`, exposé au front par CORS |
| Heure du serveur | En-tête `Date` de chaque réponse, exposé au front par CORS (calage du décompte, §4) |

### Authentification

- **Enseignant** : session HTTP côté serveur, cookie de session `HttpOnly` envoyé automatiquement par le navigateur
  (`credentials: "include"` côté front). Le front lit le jeton CSRF et le renvoie dans l'en-tête `X-XSRF-TOKEN` sur
  toute requête qui modifie des données (`POST`, `PUT`, `PATCH`, `DELETE`).
- **Étudiant** : routes `/api/public/**`. Après avoir rejoint une séance, l'étudiant reçoit un **jeton de
  participant** à renvoyer dans l'en-tête `X-Jeton-Participant`. Le jeton n'est valable que pour cette séance.
  Ces routes sont ouvertes sans session et hors CSRF : le contrôle d'accès repose sur ce jeton.
- Développement : `SECURITE_OUVERTE=true` dans `.env` ouvre toutes les routes et coupe le CSRF (jamais sur le serveur).

### Codes de statut

| Statut | Quand |
|---|---|
| `200 OK` | Lecture ou action réussie avec un corps |
| `201 Created` | Ressource créée (en-tête `Location`) |
| `204 No Content` | Action réussie sans corps |
| `400 Bad Request` | Requête mal formée (JSON illisible, paramètre au mauvais type) ou champ refusé par la validation (`@Valid`), code `REQUETE_INVALIDE` |
| `401 Unauthorized` | Pas connecté, session expirée, jeton de participant absent ou invalide |
| `403 Forbidden` | Connecté sans le droit (ressource d'un autre enseignant), jeton CSRF absent |
| `404 Not Found` | Ressource introuvable |
| `409 Conflict` | Conflit avec l'état actuel : doublon, question verrouillée, réponse hors délai ou déjà donnée, séance close |
| `413 Payload Too Large` | Fichier de plus de 50 Mo |
| `415 Unsupported Media Type` | Fichier qui n'est pas un PDF ou une image acceptée |
| `422 Unprocessable Entity` | Données bien formées refusées par une règle métier |
| `500 Internal Server Error` | Erreur imprévue ou contrainte de base violée : message générique et code `ERREUR_SERVEUR`, détail seulement dans les logs ; rien n'est enregistré |

### Format des erreurs

Toutes les erreurs, y compris 401 et 403, ont la même forme (`ReponseErreur`) :

```json
{
  "error": "Le cours 42 est introuvable.",
  "request_id": "8f0c6e0e-6a51-4c1e-9b0b-2f7f0f2d4c11"
}
```

Une erreur de validation (`400`, code `REQUETE_INVALIDE`, comme dans les tableaux d'erreurs du dossier de conception)
ajoute `champs`, un message par champ fautif :

```json
{
  "error": "Certaines valeurs sont invalides.",
  "request_id": "…",
  "champs": { "titre": "Le titre est obligatoire." },
  "code": "REQUETE_INVALIDE"
}
```

Une erreur métier qui a un code stable l'ajoute dans `code`, en majuscules. Le front s'en sert quand le statut ne
suffit pas à choisir le message (plusieurs causes de 409, par exemple). Sans code, le champ est absent :

```json
{
  "error": "Tu as déjà répondu.",
  "request_id": "…",
  "code": "DEJA_REPONDU"
}
```

## 2. Routes existantes

| Méthode | Route | Accès | Description |
|---|---|---|---|
| `GET` | `/actuator/health` | Public | Santé de l'application (sonde du reverse proxy et de Docker) |

## 3. Routes REST prévues

Légende des priorités : **M** = MUST, **S** = SHOULD, **C** = COULD. « Ens. » = enseignant connecté (propriétaire de
la ressource), « Étu. » = étudiant avec jeton de participant, « Admin » = rôle `ADMINISTRATEUR`.

### 3.1 Compte (`compte`) : UC1, UC15

| Méthode | Route | Accès | Prio | Description |
|---|---|---|---|---|
| `POST` | `/api/auth/connexion` | Public | M | Connexion enseignant `{identifiant, motDePasse}` → `200` profil + cookie de session ; `401` message générique |
| `POST` | `/api/auth/deconnexion` | Ens. | M | Ferme la session → `204` |
| `GET` | `/api/auth/moi` | Ens. | M | Profil connecté `{idEnseignant, nom, prenom, role}` ; `401` si non connecté |
| `GET` | `/api/auth/csrf` | Public | M | Fournit le jeton CSRF au front |

### 3.2 Cours et versions (`cours`) : UC2, UC3, UC6, UC11

| Méthode | Route | Accès | Prio | Description |
|---|---|---|---|---|
| `GET` | `/api/cours` | Ens. | M | Mes cours (E2) : titre, dernière version, nombre de quiz, dernière modification |
| `POST` | `/api/cours` | Ens. | M | Importer un PDF (multipart `fichier`, `titre` facultatif) → `201` cours en v1 ; `409` PDF déjà importé ; `413` ; `415` |
| `GET` | `/api/cours/{idCours}` | Ens. | M | Détail d'un cours et de ses versions |
| `PATCH` | `/api/cours/{idCours}` | Ens. | M | Renommer ou archiver `{titre?, archive?}` |
| `POST` | `/api/cours/{idCours}/versions` | Ens. | M | Nouvelle version du PDF (multipart) → `201` version + pages proposées (`valide = false`) |
| `GET` | `/api/cours/{idCours}/versions/{idVersion}/fichier` | Ens. (Étu. de la séance plus tard) | M | **Fait (éditeur E3, enseignant seulement).** Le PDF (`application/pdf`), jamais servi en accès direct (voir ci-dessous) |
| `GET` | `/api/cours/{idCours}/versions/{idVersion}/pages` | Ens. | M | **Fait (UC3, éditeur E3).** Pages insérées de la version, dans l'ordre (voir ci-dessous) |
| `POST` | `/api/cours/{idCours}/pages-quiz` | Ens. | M | **Fait (UC3).** Insérer une page de quiz (voir ci-dessous) → `201` |
| `POST` | `/api/cours/{idCours}/versions/{idVersion}/pages` | Ens. | M | Insérer une page de classement `{numeroPage, ordre, idClassement}` (UC11) → `201` |
| `PATCH` | `/api/pages/{idPage}` | Ens. | M | **Fait (éditeur E3).** Déplacer une page `{numeroPage, ordre}` → `200` pages de la version à jour |
| `DELETE` | `/api/pages/{idPage}` | Ens. | M | **Fait (règle de l'UC3 : confirmation côté front).** Retirer une page (la question n'est pas supprimée) → `204` |
| `POST` | `/api/cours/{idCours}/versions/{idVersion}/pages/validation` | Ens. | M | Valider les positions proposées après réapplication (UC6) |

**Insérer une page de quiz (UC3).** Route du dossier de conception. Enseignant connecté et propriétaire du cours,
lu dans la session (jamais dans la requête) ; un cours d'un autre enseignant répond comme un cours inexistant.

```json
{ "idVersion": 7, "numeroPage": 4, "ordre": 0, "idQuiz": 3, "repereTexte": "…fin de la page 4" }
```

- `numeroPage` : page du PDF après laquelle la page s'insère (0 = avant la première, `nbPages` = après la
  dernière) ; `ordre` : rang parmi les pages déjà insérées au même endroit (0 = juste après la page du PDF). Les
  pages de rang supérieur ou égal au même endroit sont décalées de 1.
- `idQuiz` absent : un nouveau quiz est créé (« Quiz n », non noté, 20 s, passage manuel, cours projeté seul).
  Dans tous les cas une question vierge est ajoutée (QCM, énoncé vide, durée du quiz, 1000 points, décroissance,
  sans points partiels).
- `repereTexte` : 300 caractères au plus, espaces normalisés (sert à l'UC6).

Réponse `201`, en-tête `Location: /api/cours/{idCours}/pages/{idPage}` :

```json
{
  "idPage": 12, "idQuiz": 3, "titreQuiz": "Quiz 1", "quizCree": false, "idQuestion": 40,
  "numeroPage": 4, "ordre": 0,
  "pages": [ { "idPage": 9, "numeroPage": 2, "ordre": 0, "type": "QUIZ", "idQuestion": 31, "idQuiz": 3, "titreQuiz": "Quiz 1" },
             { "idPage": 12, "numeroPage": 4, "ordre": 0, "type": "QUIZ", "idQuestion": 40, "idQuiz": 3, "titreQuiz": "Quiz 1" } ]
}
```

| Cas (UC3) | Statut | `code` | `error` |
|---|---|---|---|
| Pas connecté | `401` | — | Vous devez être connecté. |
| Corps incomplet, valeur négative, repère trop long | `400` | `REQUETE_INVALIDE` | Certaines valeurs sont invalides. (`champs`) |
| Cours inexistant ou d'un autre enseignant | `404` | `COURS_INTROUVABLE` | Cours introuvable. |
| Cours archivé | `409` | `COURS_ARCHIVE` | Ce cours est archivé : il n'est plus modifiable. |
| Version qui n'est plus la dernière | `409` | `VERSION_PERIMEE` | Une nouvelle version du cours a été importée : rechargez l'éditeur. |
| Emplacement hors du cours | `422` | `EMPLACEMENT_INVALIDE` | Emplacement invalide : rechargez l'éditeur. |
| Quiz inexistant, archivé ou d'un autre cours | `404` | `QUIZ_INTROUVABLE` | Quiz introuvable. |
| Quiz en cours de séance | `409` | `QUIZ_EN_SEANCE` | Ce quiz est en cours de séance : réessayez après sa clôture. |
| Erreur de base de données (rien n'est créé) | `500` | `ERREUR_SERVEUR` | Message générique |

**PDF d'une version (éditeur E3).** `GET /api/cours/{idCours}/versions/{idVersion}/fichier`, lu par PDF.js pour
l'aperçu, les miniatures et le repère de texte de l'UC3. Enseignant propriétaire seulement ; l'accès des étudiants
viendra avec l'affichage du cours en séance.

- `200`, corps = le PDF, `Content-Type: application/pdf`, `Content-Disposition: inline`,
  `X-Content-Type-Options: nosniff`, `Cache-Control: private`.
- Requêtes `Range` prises en charge (`206 Partial Content`) : PDF.js lit le fichier par tranches.
- `404` `COURS_INTROUVABLE` (cours inexistant ou d'un autre enseignant), `404` `VERSION_INTROUVABLE` (version d'un
  autre cours), `404` `FICHIER_INTROUVABLE` (fichier supprimé par l'administrateur, UC20, ou absent du disque).
- Le chemin enregistré en base est lu dans le dossier de stockage (`STOCKAGE_DOSSIER`) ; un chemin absolu ou avec
  `../` n'est jamais lu.

**Pages d'une version (éditeur E3).** Une page se décrit partout de la même façon (réponse du `POST`, du `GET` et du
`PATCH`) : `{idPage, numeroPage, ordre, type, idQuestion, idQuiz, titreQuiz}`, `type` = `QUIZ` ou `CLASSEMENT`
(`idQuestion`, `idQuiz` et `titreQuiz` nuls pour une page de classement). Lire ou paramétrer la question elle-même
relève de l'UC4/UC5 (§3.3).

- `GET /api/cours/{idCours}/versions/{idVersion}/pages` → `200` tableau dans l'ordre (page du PDF, puis rang). Toute
  version du cours se lit. `404` `COURS_INTROUVABLE` (cours inexistant ou d'un autre enseignant),
  `404` `VERSION_INTROUVABLE` (version d'un autre cours).
- `DELETE /api/pages/{idPage}` → `204`. La question et le quiz sont conservés, même si le quiz n'a plus de page. Les
  pages restées au même endroit sont renumérotées (rangs 0, 1, 2…). Action sensible journalisée. Le front demande
  une confirmation avant l'appel.
- `PATCH /api/pages/{idPage}` `{numeroPage, ordre}` → `200` pages de la version à jour. `ordre` est le rang parmi les
  autres pages déjà à cet endroit (de 0 à leur nombre) ; l'ancien et le nouvel endroit sont renumérotés.

| Cas (retirer, déplacer) | Statut | `code` |
|---|---|---|
| Page inexistante ou d'un cours d'un autre enseignant | `404` | `PAGE_INTROUVABLE` |
| Cours archivé | `409` | `COURS_ARCHIVE` |
| Page d'une version qui n'est plus la dernière | `409` | `VERSION_PERIMEE` |
| Page d'un quiz en cours de séance (ses questions changeraient de position) | `409` | `QUIZ_EN_SEANCE` |
| Emplacement hors du cours (`PATCH`) | `422` | `EMPLACEMENT_INVALIDE` |
| Corps incomplet ou négatif (`PATCH`) | `400` | `REQUETE_INVALIDE` |

### 3.3 Quiz et questions (`quiz`) : UC4, UC5, UC13, UC14

| Méthode | Route | Accès | Prio | Description |
|---|---|---|---|---|
| `GET` | `/api/cours/{idCours}/quiz` | Ens. | M | Quiz du cours |
| `POST` | `/api/cours/{idCours}/quiz` | Ens. | M | Créer un quiz `{titre, estNote, dureeParDefautSec, passageAutomatique, delaiPassageAutoSec, affichageCours}` → `201` |
| `GET` | `/api/quiz/{idQuiz}` | Ens. | M | Quiz avec ses questions **et** les réponses attendues (vue éditeur E3) + indicateur « prêt à lancer » |
| `PATCH` | `/api/quiz/{idQuiz}` | Ens. | M | Modifier le quiz (mode noté, durée, déroulé) ; `409` si une séance est en cours |
| `DELETE` | `/api/quiz/{idQuiz}` | Ens. | M | Archiver le quiz → `204` |
| `POST` | `/api/quiz/{idQuiz}/questions` | Ens. | M | Créer une question (voir exemple ci-dessous) → `201` |
| `PUT` | `/api/questions/{idQuestion}` | Ens. | M | Remplacer une question ; `409` si verrouillée |
| `DELETE` | `/api/questions/{idQuestion}` | Ens. | M | Archiver une question → `204` |
| `POST` | `/api/questions/{idQuestion}/motifs/test` | Ens. | S | Tester des motifs sur un exemple `{motifs, exemple}` → `{accepte, motifQuiAccepte}` |
| `PUT` | `/api/questions/{idQuestion}/image` | Ens. | C | Ajouter ou remplacer l'image (PNG, JPEG, WebP) |
| `DELETE` | `/api/questions/{idQuestion}/image` | Ens. | C | Retirer l'image → `204` |

Exemple de création d'une question QCM :

```json
{
  "type": "QCM",
  "enonce": "Quel protocole sert à l'annuaire de l'IUT ?",
  "dureeSec": 20,
  "bareme": { "pointsMax": 1000, "decroissanceTemps": true, "pointsPartiels": false },
  "propositions": [
    { "libelle": "LDAP", "estCorrecte": true },
    { "libelle": "SMTP", "estCorrecte": false }
  ],
  "ecransIntermediaires": [
    { "type": "BONNE_REPONSE", "delaiApresClotureSec": 0 },
    { "type": "CLASSEMENT", "delaiApresClotureSec": 5 }
  ]
}
```

Pour `POINTS_A_RELIER`, `propositions` est remplacé par `paires: [{gauche, droite}]` ; pour `REPONSE_LIBRE`, par
`motifs: [{motif, mode: "SIMPLE" | "REGEX"}]`.

### 3.4 Séances, côté enseignant (`seance`) : UC7, UC14

| Méthode | Route | Accès | Prio | Description |
|---|---|---|---|---|
| `POST` | `/api/quiz/{idQuiz}/sessions` | Ens. | M | Lancer une séance `{idVersion}` → `201` `{idSession, codeAcces, lien}` ; `422` si une question est incomplète |
| `GET` | `/api/sessions/{idSession}` | Ens. | M | État complet (E5) : question en cours, échéance, connectés, répartition |
| `GET` | `/api/sessions/{idSession}/qrcode` | Ens. | M | QR code du lien (`image/png`), généré par le serveur |
| `POST` | `/api/sessions/{idSession}/question-suivante` | Ens. | M | Ouvre la question suivante (ou passe à la page suivante) |
| `POST` | `/api/sessions/{idSession}/cloture-question` | Ens. | M | Clôt la question en cours avant l'échéance |
| `POST` | `/api/sessions/{idSession}/cloture` | Ens. | M | Clôt la séance ; le code d'accès devient invalide |
| `POST` | `/api/sessions/{idSession}/identites-non-verifiees` | Ens. | S | Autorise l'entrée « identité non vérifiée » si le LDAP ne répond pas `{autorise}` |

### 3.5 Séances, côté étudiant (`seance`) : UC8, UC9, UC12, UC15

| Méthode | Route | Accès | Prio | Description |
|---|---|---|---|---|
| `GET` | `/api/public/sessions/{codeAcces}` | Public | M | Titre du cours et du quiz, mode noté, état ; `404` code inconnu ; `409` séance close |
| `POST` | `/api/public/sessions/{codeAcces}/participants` | Public | M | Rejoindre sans compte `{pseudonyme}` → `201` `{idParticipant, jeton, pseudonyme}` ; `409` pseudonyme pris (avec suggestion) |
| `POST` | `/api/public/sessions/{codeAcces}/participants/ldap` | Public | S | Rejoindre un quiz noté `{identifiant, motDePasse, pseudonyme?}` → `201` ; `401` message générique |
| `POST` | `/api/public/sessions/{codeAcces}/reconnexion` | Étu. | M | Reprendre après une coupure → état courant + score |
| `GET` | `/api/public/sessions/{codeAcces}/etat` | Étu. | M | Question en cours **sans réponses attendues**, échéance, score et rang du participant |
| `POST` | `/api/public/sessions/{codeAcces}/reponses` | Étu. | M | Répondre (voir ci-dessous) → `201` `{pointsProvisoires}` ; `409` hors délai, déjà répondu ou mauvaise question |

Corps d'une réponse, selon le type :

```json
{ "idQuestion": 12, "propositions": [41, 43] }
{ "idQuestion": 13, "liens": [ { "gauche": 7, "droite": 9 } ] }
{ "idQuestion": 14, "texte": "protocole LDAP" }
```

Le temps de réponse est **mesuré par le serveur** ; aucun horodatage envoyé par le client n'est pris en compte.

### 3.6 Classements (`classement`) : UC10, UC11, UC12

| Méthode | Route | Accès | Prio | Description |
|---|---|---|---|---|
| `GET` | `/api/classements` | Ens. | M | Mes classements : nom, nombre de quiz, nombre de cours |
| `POST` | `/api/classements` | Ens. | M | Créer `{nom, idQuiz: []}` → `201` ; `409` nom déjà utilisé |
| `GET` | `/api/classements/{idClassement}` | Ens. | M | Détail et lignes du classement (pseudonymes et scores) |
| `PUT` | `/api/classements/{idClassement}` | Ens. | S | Renommer, relier ou délier des quiz (recalcul) |
| `DELETE` | `/api/classements/{idClassement}` | Ens. | S | Supprimer → `204` |
| `GET` | `/api/sessions/{idSession}/classement` | Ens. | M | Classement de la séance (E6), avec l'évolution des rangs |

### 3.7 Résultats et correction (`resultat`) : UC16, UC17, UC18

| Méthode | Route | Accès | Prio | Description |
|---|---|---|---|---|
| `GET` | `/api/cours/{idCours}/sessions` | Ens. | S | Séances du cours : date, quiz, nombre de participants |
| `GET` | `/api/sessions/{idSession}/resultats` | Ens. | S | Résultats par question et par participant (E10) |
| `GET` | `/api/sessions/{idSession}/questions/{idQuestion}/reponses-libres` | Ens. | S | Groupes de réponses identiques avec leur statut (E11) |
| `PUT` | `/api/sessions/{idSession}/questions/{idQuestion}/corrections` | Ens. | S | Décisions `[{texteNormalise, decision: "VALIDEE" | "REFUSEE"}]` |
| `POST` | `/api/sessions/{idSession}/questions/{idQuestion}/motifs` | Ens. | S | Ajouter un motif et l'appliquer aux réponses de la séance |
| `POST` | `/api/sessions/{idSession}/recalcul` | Ens. | S | Recalcule les scores et les classements → résumé des changements |
| `GET` | `/api/sessions/{idSession}/export.csv` | Ens. | S | Export CSV (`text/csv`), journalisé |

### 3.8 Administration (`administration`)

| Méthode | Route | Accès | Prio | Description |
|---|---|---|---|---|
| `DELETE` | `/api/admin/sessions/{idSession}` | Admin | S | Supprimer une séance et ses données (droit à l'oubli) |
| `DELETE` | `/api/admin/participants/{idParticipant}` | Admin | S | Supprimer les données d'un participant |
| `GET` | `/api/admin/fichiers/orphelins` | Admin | C | Images sans question, fichiers de versions anciennes |
| `DELETE` | `/api/admin/versions/{idVersion}/fichier` | Admin | C | Supprimer le fichier d'une version (la ligne reste) |

La purge à échéance (3 mois, 12 mois, 6 mois) est une tâche planifiée, sans route.

## 4. Temps réel (WebSocket / STOMP)

- Point d'entrée : `/ws` (WebSocket, STOMP). Préfixe des destinations applicatives : `/app` ; abonnements :
  `/topic` (diffusion) et `/user/queue` (message personnel).
- Le serveur **pousse** ; les commandes passent par l'API REST (§3.4, §3.5).
- Authentification : cookie de session pour l'enseignant ; en-tête STOMP `X-Jeton-Participant` dans la trame
  `CONNECT` pour l'étudiant. Un abonnement à une séance d'un autre enseignant est refusé.

| Destination | Abonnés | Contenu | Quand |
|---|---|---|---|
| `/topic/sessions/{codeAcces}/etat` | Tous | `{etat, position, idQuestion, echeance}` | Changement d'état, ouverture d'une question |
| `/topic/sessions/{codeAcces}/question` | Tous | Énoncé, propositions **sans** `estCorrecte`, durée, échéance | Ouverture d'une question |
| `/topic/sessions/{codeAcces}/connectes` | Enseignant | `{nombre}` | Entrée ou départ d'un participant |
| `/topic/sessions/{codeAcces}/repartition` | Enseignant | Nombre de réponses par proposition, total reçu | À chaque réponse (regroupé, au plus 1 envoi toutes les 300 ms) |
| `/topic/sessions/{codeAcces}/correction` | Tous | Bonnes réponses, répartition finale | Clôture de la question |
| `/topic/sessions/{codeAcces}/ecran` | Enseignant | Écran intermédiaire à afficher | Après la clôture, selon la configuration |
| `/topic/sessions/{codeAcces}/classement` | Enseignant | Classement complet (pseudonymes, scores, évolution) | Après la clôture de chaque question |
| `/user/queue/resultat` | Un étudiant | `{points, scoreTotal, rang, nombreParticipants, evolution}` | Après la clôture de chaque question |

Exemple de message `/topic/sessions/{codeAcces}/question` :

```json
{
  "idQuestion": 12,
  "position": 3,
  "type": "QCM",
  "enonce": "Quel protocole sert à l'annuaire de l'IUT ?",
  "propositions": [ { "idProposition": 41, "libelle": "LDAP" }, { "idProposition": 42, "libelle": "SMTP" } ],
  "dureeSec": 20,
  "echeance": "2026-11-12T14:05:20"
}
```

Le front calcule le temps restant à partir d'`echeance` et de l'heure du serveur (en-tête `Date` d'une réponse
REST), pour ne pas dépendre de l'horloge du téléphone.

## 5. Documentation interactive

À ajouter : `springdoc-openapi`, Swagger UI sur `/swagger-ui.html` et le schéma sur
`/v3/api-docs`, accessibles en développement et protégés en production.
