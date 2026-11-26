# API du back-end

Contrat entre le back-end Spring Boot et le front React : routes REST et canal temps réel (WebSocket/STOMP).

> **État actuel** : le socle (`GET /actuator/health`, format d'erreur, sécurité, CORS) et l'UC8 « Rejoindre un
> quiz » (§2). Les autres routes ci-dessous sont la **cible** proposée pour les features ; elles sont marquées par
> leur cas d'utilisation (UC) et leur priorité. Toute route ajoutée, renommée ou supprimée doit être reportée ici
> dans la même pull request, et annoncée à l'équipe front. La documentation Swagger/OpenAPI générée depuis le code (exigée à la soutenance 2)
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
| `500 Internal Server Error` | Erreur imprévue (message générique, détail seulement dans les logs) |

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
| `GET` | `/api/public/sessions/{codeAcces}` | Public | UC8. Ouverture du lien ou du QR code (écran E7) : `200` `{titreCours, titreQuiz, estNotee, etat}` avec `etat` = `PREPAREE` ou `EN_COURS` ; `404` code inconnu ; `409` séance close. Si `estNotee` est vrai, le front propose la connexion avec le compte IUT |
| `POST` | `/api/public/sessions/{codeAcces}/participants` | Public | UC8. Entrer dans la séance avec un pseudonyme `{pseudonyme}` (écran E7). `201` `{idParticipant, jeton, pseudonyme}` avec `Cache-Control: no-store` ; `400` corps illisible, ou pseudonyme absent ou hors taille (`REQUETE_INVALIDE`, `champs.pseudonyme`) ; `404` code inconnu ; `409` séance close, quiz noté (« connecte-toi avec ton compte IUT ») ou pseudonyme pris (champ `suggestion`) ; `422` pseudonyme refusé après nettoyage (`champs.pseudonyme`) |

#### Rejoindre un quiz (UC8)

- **Pseudonyme** : nettoyé par le serveur (espaces de début et de fin retirés, suites d'espaces réduites à une,
  normalisation Unicode NFC), puis 2 à 20 caractères, sans caractère de contrôle. Le pseudonyme **retenu** est celui
  de la réponse (`Alice` pour `"  Alice  "`). L'unicité est vérifiée dans la séance, à l'identique :
  `Alice` et `alice` sont deux pseudonymes *(à valider avec le client)*.
- **Suggestion** : en cas de doublon, le serveur ajoute 2, 3, 4… à la fin (`Alice2`), en raccourcissant pour rester
  à 20 caractères. Si deux étudiants envoient le même pseudonyme au même instant, le second reçoit le même `409`
  sans `suggestion` : le front redemande un pseudonyme.
- **Jeton** : 43 caractères (Base64url). Le serveur n'en garde que l'empreinte SHA-256 : il est remis **une seule
  fois**, et la réponse qui le porte n'est jamais mise en cache (`Cache-Control: no-store`). Le front le garde pour ce
  code d'accès, l'envoie ensuite dans `X-Jeton-Participant` et dans la trame STOMP `CONNECT`, puis lit l'état par
  `GET /api/public/sessions/{codeAcces}/etat` *(UC9)* : la question en cours si le quiz a déjà commencé, sinon
  l'attente de la première question.
- **Temps réel** : une fois l'entrée validée, le serveur pousse `{nombre}` sur
  `/topic/sessions/{codeAcces}/connectes` (compteur de l'écran E5). Une entrée refusée ne pousse rien. La poignée de
  main `/ws` est publique ; le contrôle du jeton à la connexion STOMP et des abonnements arrive avec l'UC9.
- **Format du code d'accès** : 32 caractères au plus, lu dans l'adresse ; il est secret tant que la séance dure.
- **Messages d'erreur** : tutoyés, sans pseudonyme, jeton ni code d'accès.

Un conflit (409) qui sait proposer une valeur de remplacement ajoute `suggestion` (aujourd'hui : un pseudonyme libre
quand celui qui est demandé est déjà pris). Le champ est absent quand il n'y a rien à proposer :

```json
{
  "error": "Ce pseudonyme est déjà pris. Essaie celui qui t'est proposé.",
  "request_id": "…",
  "suggestion": "Alice2"
}
```

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

**Fait (UC2) : `POST /api/cours`**, importer un PDF (écran E2). Enseignant connecté, jeton CSRF,
`multipart/form-data` :

| Champ | Contenu |
|---|---|
| `fichier` | Le PDF, obligatoire, 50 Mo au plus *(à confirmer avec le client)* |
| `titre` | 1 à 200 caractères après retrait des espaces autour, obligatoire (le front propose le nom du fichier) |
| `confirmerDoublon` | Booléen facultatif, `false` par défaut : importer un PDF déjà présent dans un autre cours |

`201`, `Location: /api/cours/{idCours}` :

```json
{ "idCours": 4, "titre": "Réseaux", "numeroVersion": 1, "nbPages": 12, "nbQuiz": 0,
  "dateModification": "2026-11-25T14:00:00" }
```

| Cas (UC2) | Statut | `code` | `error` |
|---|---|---|---|
| Fichier absent, `confirmerDoublon` illisible | `400` | — | La requête est mal formée. |
| Requête qui n'est pas en `multipart/form-data` | `415` | — | Ce type de contenu n'est pas pris en charge. |
| Fichier qui n'est pas un PDF | `415` | `FICHIER_NON_PDF` | Le fichier n'est pas un PDF. |
| Fichier de plus de 50 Mo | `413` | — | Le fichier envoyé est trop volumineux. |
| Titre absent, vide ou trop long (`champs.titre`) | `422` | `TITRE_INVALIDE` | Le titre est obligatoire (200 caractères au plus). |
| PDF corrompu, chiffré ou sans page (`champs.fichier`) | `422` | `PDF_ILLISIBLE` | Le PDF est illisible ou protégé par un mot de passe. |
| PDF déjà importé par cet enseignant dans un cours non archivé | `409` | `DOUBLON_COURS` | Vous avez déjà importé ce PDF dans le cours « … ». Importer quand même ? |
| Disque ou base en panne (rien n'est créé) | `500` | — | Message générique ; le front affiche « L'import a échoué, réessayez. » |

Le contrôle porte sur le contenu (signature `%PDF-` au début, lecture par PDFBox), pas sur le nom ni sur le type
annoncé. Le fichier est rangé sous un nom tiré au hasard, dans un dossier qu'aucune route ne sert. Au-delà de 100 Mo,
le serveur coupe la connexion au lieu de répondre `413` : le front contrôle la taille avant d'envoyer.

**Fait (UC2) : `GET /api/cours` et `GET /api/cours/{idCours}`**, liste « Mes cours » (écran E2) et bouton
« Ouvrir ». Enseignant connecté ; il ne voit que ses propres cours. Un cours archivé (par l'administrateur, UC20)
n'est **plus listé**, mais son propriétaire peut encore l'**ouvrir**, en lecture seule.

`GET /api/cours` → `200`, un tableau sans enveloppe (vide s'il n'a pas de cours), chaque élément ayant la forme de la
réponse de l'import, le cours modifié le plus récemment en premier *(à valider)* :

```json
[ { "idCours": 4, "titre": "Réseaux", "numeroVersion": 1, "nbPages": 12, "nbQuiz": 0,
    "dateModification": "2026-11-25T14:00:00" } ]
```

`GET /api/cours/{idCours}` → `200`, les mêmes champs plus `idVersion`, la dernière version : celle que l'éditeur (E3)
ouvre, où insérer une page (UC3) et sur laquelle lancer un quiz (UC7) ; et `archive` (booléen) : s'il vaut `true`, le
front ouvre l'éditeur en lecture seule :

```json
{ "idCours": 4, "titre": "Réseaux", "idVersion": 9, "numeroVersion": 1, "nbPages": 12, "nbQuiz": 0,
  "dateModification": "2026-11-25T14:00:00", "archive": false }
```

`numeroVersion` et `nbPages` (pages du PDF) sont ceux de la dernière version. `nbQuiz` compte les quiz non archivés
du cours : un quiz appartient au cours, pas à une version. `dateModification` est celle du cours.

| Cas (liste et ouverture) | Statut | `code` | `error` |
|---|---|---|---|
| Pas connecté | `401` | — | Vous devez être connecté. |
| `idCours` qui n'est pas un nombre | `400` | — | La requête est mal formée. |
| Cours inexistant ou d'un autre enseignant | `404` | `COURS_INTROUVABLE` | Cours introuvable. |

| Méthode | Route | Accès | Prio | Description |
|---|---|---|---|---|
| `GET` | `/api/cours` | Ens. | M | **Fait (UC2), détail ci-dessus.** Mes cours (E2) : titre, dernière version, nombre de quiz, dernière modification |
| `POST` | `/api/cours` | Ens. | M | **Fait (UC2), détail ci-dessus.** Importer un PDF → `201` cours en v1 ; `409` PDF déjà importé ; `413` ; `415` ; `422` |
| `GET` | `/api/cours/{idCours}` | Ens. | M | **Fait (UC2), détail ci-dessus.** Cours et sa dernière version ; la liste de ses versions viendra avec l'UC6 |
| `PATCH` | `/api/cours/{idCours}` | Ens. | M | Renommer ou archiver `{titre?, archive?}` |
| `POST` | `/api/cours/{idCours}/versions` | Ens. | M | Nouvelle version du PDF (multipart) → `201` version + pages proposées (`valide = false`) |
| `GET` | `/api/cours/{idCours}/versions/{idVersion}/fichier` | Ens. ou Étu. de la séance | M | Le PDF (`application/pdf`), jamais servi en accès direct |
| `GET` | `/api/cours/{idCours}/versions/{idVersion}/pages` | Ens. | M | Pages insérées de la version, dans l'ordre |
| `POST` | `/api/cours/{idCours}/versions/{idVersion}/pages` | Ens. | M | Insérer une page `{numeroPage, ordre, idQuestion | idClassement}` → `201` |
| `PATCH` | `/api/pages/{idPage}` | Ens. | M | Déplacer une page `{numeroPage, ordre}` |
| `DELETE` | `/api/pages/{idPage}` | Ens. | M | Retirer une page (la question n'est pas supprimée) → `204` |
| `POST` | `/api/cours/{idCours}/versions/{idVersion}/pages/validation` | Ens. | M | Valider les positions proposées après réapplication (UC6) |

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
| `POST` | `/api/quiz/{idQuiz}/sessions` | Ens. | M | **Fait (UC7).** Lancer une séance (corps facultatif `{autoriserIdentiteNonVerifiee}`) → `201`, `Location: /api/sessions/{idSession}` |
| `GET` | `/api/sessions/ouverte` | Ens. | M | **Fait (UC7).** La séance non terminée de l'enseignant, à reprendre ou à terminer ; `404` s'il n'y en a pas |
| `GET` | `/api/sessions/{idSession}` | Ens. | M | **Fait (UC7).** État de la séance (E5) |
| `POST` | `/api/sessions/{idSession}/questions/suivante` | Ens. | M | **Fait (UC7).** Ouvre la question suivante (la première après le lancement) ; après la dernière, termine la séance |
| `POST` | `/api/sessions/{idSession}/questions/courante/cloture` | Ens. | M | **Fait (UC7).** Clôt la question en cours avant la fin du temps |
| `POST` | `/api/sessions/{idSession}/cloture` | Ens. | M | **Fait (UC7).** Termine la séance ; le lien et le QR code ne valent plus rien |
| `POST` | `/api/sessions/{idSession}/identites-non-verifiees` | Ens. | S | Autorise l'entrée « identité non vérifiée » si le LDAP ne répond pas `{autorise}` *(prévue, UC15)* |

#### Lancer et dérouler une séance (UC7)

Toutes ces routes répondent avec `Cache-Control: no-store` (le code d'accès est secret) et sont réservées à
l'enseignant propriétaire. Les `POST` du déroulement n'ont pas de corps et renvoient l'état à jour (`200`).

**Lancement.** `201` :

```json
{
  "idSession": 7, "idQuiz": 3, "titre": "Quiz du chapitre 3", "statut": "PREPAREE", "modeNote": false,
  "nbQuestions": 3, "codeAcces": "q7Vb0m3yR1c8Xk2LdT9aZw",
  "lienAcces": "http://localhost:5173/rejoindre/q7Vb0m3yR1c8Xk2LdT9aZw",
  "qrCode": "iVBORw0KGgo…", "dateLancement": "2026-11-25T14:00:00+01:00"
}
```

- La séance présente la **dernière version** du cours. Ses questions sont les pages de quiz de ce quiz dans cette
  version, triées par page du PDF puis par ordre ; `position` est leur rang (1 pour la première).
- `lienAcces` = `<FRONT_URL>/rejoindre/<codeAcces>` ; `qrCode` est une image PNG en base64
  (`data:image/png;base64,…`), produite par l'application. Il n'y a pas de route séparée pour le QR code.
- Les questions lancées sont verrouillées (`question.verrouillee`). Une seule séance ouverte par enseignant.

**État** (`GET` et réponses des `POST`). Les champs sans valeur sont absents :

```json
{
  "idSession": 7, "idQuiz": 3, "titre": "Quiz du chapitre 3", "statut": "EN_COURS", "modeNote": false,
  "nbQuestions": 3, "position": 1, "codeAcces": "…", "lienAcces": "…", "qrCode": "…",
  "dateLancement": "2026-11-25T14:00:00+01:00", "connectes": 24, "questionOuverte": true,
  "question": { "idQuestion": 12, "position": 1, "type": "QCM", "enonce": "…", "dureeSec": 20,
    "echeance": "2026-11-25T14:01:20+01:00", "choixMultiple": false,
    "propositions": [ { "idProposition": 41, "libelle": "LDAP" } ] },
  "tempsRestantMs": 14200,
  "repartition": { "idQuestion": 12, "total": 9, "propositions": [ { "idProposition": 41, "nombre": 9 } ] }
}
```

- `question` a la forme de `GET /api/public/sessions/{codeAcces}/etat` (UC9) : sans aucune réponse attendue.
- `correction` (même forme que le message `…/correction` ci-dessous) n'est présente qu'une fois la question close.
- Séance terminée : `statut = CLOTUREE`, `dateCloture`, plus de `lienAcces` ni de `qrCode`.

| Cas (UC7) | Statut | `code` | `error` |
|---|---|---|---|
| Pas connecté | `401` | — | Vous devez être connecté. |
| Quiz inexistant ou archivé | `404` | — | Ce quiz n'existe pas ou a été supprimé. |
| Quiz ou séance d'un autre enseignant, jeton CSRF absent | `403` | — | Action non autorisée. |
| Quiz sans page de question, ou question incomplète (au lancement ou à son ouverture) | `422` | `QUIZ_INCOMPLET` | Le quiz contient une question incomplète (page 6). Complétez-la avant de lancer. |
| Séance déjà ouverte | `409` | `SESSION_DEJA_OUVERTE` | Une session est déjà ouverte pour le cours « … ». La reprendre ou la terminer ? |
| Quiz noté, annuaire indisponible, sans autorisation | `409` | `ANNUAIRE_INDISPONIBLE` | L'annuaire de l'IUT est indisponible. Autoriser l'entrée en identité non vérifiée ? |
| QR code, code d'accès, base ou canal temps réel en panne (aucune séance créée) | `500` | — | Message générique ; le front affiche « Le lancement a échoué, réessayez. » |
| Séance inexistante | `404` | `SEANCE_INTROUVABLE` | Cette séance n'existe pas ou a été supprimée. |
| Séance terminée | `409` | `SEANCE_TERMINEE` | Cette séance est terminée. |
| « Suivant » pendant une question ouverte | `409` | `QUESTION_DEJA_OUVERTE` | Une question est déjà ouverte. Clôturez-la avant de passer à la suivante. |
| Clôture sans question ouverte | `409` | `AUCUNE_QUESTION_OUVERTE` | Aucune question n'est ouverte. |

**Temps réel du déroulement.** Les envois partent une fois le changement enregistré.

| Événement du dossier | Destination | Contenu |
|---|---|---|
| Ouverture, clôture, `session_terminee` | `…/etat` | `{etat, position, idQuestion, echeance}` ; `echeance` nulle quand la question est close ; `etat = CLOTUREE` à la fin |
| `question_demarree` | `…/question` | La question, forme ci-dessus |
| `question_close` | `…/correction` | `{idQuestion, position, type, bonnesPropositions, paires, reponsesAcceptees, total, repartition}` |
| `question_close` | `/user/queue/resultat` | `{idQuestion, points, scoreTotal, rang, nombreParticipants, evolution, repondu, bonneReponse}` |
| `ecran_intermediaire` | `…/ecran` | `{idQuestion, position, type, rang, delaiApresClotureSec}`, envoyé à l'heure de l'écran |

`…` désigne `/topic/sessions/{codeAcces}`. Correction : `paires` = `[{gauche, droite, libelleGauche,
libelleDroite}]` (identifiants de paires) ; `reponsesAcceptees` = motifs simples seulement ; `repartition` =
`[{idProposition, nombre}]`. Une question se ferme **seule** à son échéance (horloge du serveur, 250 ms de marge) ;
avec le passage automatique, la suivante s'ouvre après le dernier écran puis le délai réglé (5 s sans réglage).

### 3.5 Séances, côté étudiant (`seance`) : UC8, UC9, UC12, UC15

| Méthode | Route | Accès | Prio | Description |
|---|---|---|---|---|
| `GET` | `/api/public/sessions/{codeAcces}` | Public | M | **Fait (§2).** Titre du cours et du quiz, mode noté, état ; `404` code inconnu ; `409` séance close |
| `POST` | `/api/public/sessions/{codeAcces}/participants` | Public | M | **Fait (§2).** Rejoindre sans compte `{pseudonyme}` → `201` `{idParticipant, jeton, pseudonyme}` ; `409` séance close, quiz noté ou pseudonyme pris (avec `suggestion`) ; `400` / `422` pseudonyme invalide |
| `POST` | `/api/public/sessions/{codeAcces}/participants/ldap` | Public | S | Rejoindre un quiz noté `{identifiant, motDePasse, pseudonyme?}` → `201` ; `401` message générique |
| `POST` | `/api/public/sessions/{codeAcces}/reconnexion` | Étu. | M | Reprendre après une coupure → état courant + score |
| `GET` | `/api/public/sessions/{codeAcces}/etat` | Étu. | M | **Fait (UC9).** Question ouverte **sans réponses attendues**, déjà répondu, score (voir ci-dessous) ; `401` `JETON_INVALIDE` |
| `POST` | `/api/public/sessions/{codeAcces}/reponses` | Étu. | M | **Fait (UC9).** Répondre (voir ci-dessous) → `201` `{idQuestion, horodatageServeur}`, sans points ni justesse ; erreurs ci-dessous |

Les deux routes de l'UC9 lisent le participant dans l'en-tête `X-Jeton-Participant` et répondent avec
`Cache-Control: no-store`.

**Répondre.** Corps selon le type de la question (un seul des trois contenus est lu) :

```json
{ "idQuestion": 12, "idPropositions": [41, 43] }
{ "idQuestion": 13, "liens": [ { "gauche": 7, "droite": 9 } ] }
{ "idQuestion": 14, "texteLibre": "protocole LDAP" }
```

- `liens` : `gauche` et `droite` sont des identifiants de **paires** (celle dont l'étudiant a choisi l'élément de
  gauche, celle dont il a choisi l'élément de droite). Un lien est juste quand les deux sont égaux. Chaque élément
  est relié au plus une fois ; une réponse partielle est acceptée (points partiels si le barème le permet).
- QCM : plusieurs propositions seulement si `choixMultiple` est vrai.
- Texte libre : non vide, 1000 caractères au plus.

Le temps de réponse est **mesuré par le serveur** ; aucun horodatage envoyé par le client n'est pris en compte.

| Cas (UC9) | Statut | `code` | `error` |
|---|---|---|---|
| Jeton absent, inconnu, remplacé ou d'une autre séance | `401` | `JETON_INVALIDE` | Ta session a expiré, rejoins le quiz à nouveau. |
| Séance non en cours, ou question qui n'est pas la question ouverte | `409` | `QUESTION_CLOSE` | Cette session est terminée. |
| Réponse après l'échéance (horloge du serveur) | `409` | `HORS_DELAI` | Temps écoulé : réponse non prise en compte. |
| Réponse déjà enregistrée | `409` | `DEJA_REPONDU` | Tu as déjà répondu. |
| Contenu incohérent avec la question | `422` | `REPONSE_INVALIDE` | Réponse incomplète, vérifie ta saisie. |
| `idQuestion` absent | `400` | `REQUETE_INVALIDE` | Certaines valeurs sont invalides. (`champs.idQuestion`) |
| Corps illisible | `400` | — | La requête est mal formée. |
| Erreur de base de données (rien n'est enregistré) | `500` | — | Message générique |

**État.** Lu au chargement de E8 et après chaque reconnexion :

```json
{
  "question": {
    "idQuestion": 12, "position": 1, "type": "QCM", "enonce": "…", "dureeSec": 20,
    "echeance": "2026-11-24T15:05:20+01:00",
    "propositions": [ { "idProposition": 41, "libelle": "LDAP" } ], "choixMultiple": false
  },
  "dejaRepondu": false,
  "scoreTotal": 0
}
```

- `question` vaut `null` s'il n'y a pas de question ouverte.
- `POINTS_A_RELIER` : `elementsGauche` et `elementsDroite` (`[{id, libelle}]`, `id` = identifiant de la paire),
  mélangés séparément, dans le même ordre pour tous et à chaque lecture. `REPONSE_LIBRE` : ni propositions ni
  éléments.
- **Question ouverte** (hypothèse à confirmer avec l'UC7) : la page de quiz de rang `position_courante` (1 pour la
  première) de la version projetée, pages triées par page du PDF puis par ordre ; ouverte si la séance est
  `EN_COURS` et qu'`echeance_question` est fixée.

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
- **Fait (UC9)** : la poignée de main `/ws` est publique, le contrôle se fait dans la trame STOMP.
  - `CONNECT` avec un jeton inconnu, ou sans jeton hors mode développement (`SECURITE_OUVERTE`) : trame `ERROR`
    dont l'en-tête `message` vaut `JETON_INVALIDE`. La connexion de l'enseignant arrivera avec la feature `compte`.
  - Un étudiant ne s'abonne qu'à `/topic/sessions/{sonCode}/etat`, `/question`, `/correction` et à
    `/user/queue/**` ; tout autre abonnement reçoit une trame `ERROR` `ABONNEMENT_REFUSE`.
  - Envoi d'une réponse par STOMP (dossier de conception) : `SEND /app/sessions/{idSession}/reponses`, même corps
    que la route REST plus `requestId`. Accusé `{requestId, idQuestion, horodatageServeur}` sur
    `/user/queue/reponses`, erreurs `{requestId, code, statut, error}` sur `/user/queue/erreurs` (mêmes codes que
    §3.5, `REQUETE_INVALIDE` 400 pour un message mal formé). Le front actuel répond en REST : les deux chemins
    appellent le même cas d'usage.

| Destination | Abonnés | Contenu | Quand |
|---|---|---|---|
| `/topic/sessions/{codeAcces}/etat` | Tous | `{etat, position, idQuestion, echeance}` | Changement d'état, ouverture d'une question |
| `/topic/sessions/{codeAcces}/question` | Tous | Énoncé, propositions **sans** `estCorrecte`, durée, échéance | Ouverture d'une question |
| `/topic/sessions/{codeAcces}/connectes` | Enseignant | `{nombre}` | Entrée ou départ d'un participant |
| `/topic/sessions/{codeAcces}/repartition` | Enseignant | **Fait (UC9).** `{idQuestion, total, propositions: [{idProposition, nombre}]}` (`propositions` vide hors QCM) | Après chaque réponse validée (regroupé, au plus 1 envoi toutes les 300 ms) |
| `/topic/sessions/{codeAcces}/correction` | Tous | Bonnes réponses, répartition finale | Clôture de la question |
| `/topic/sessions/{codeAcces}/ecran` | Enseignant | Écran intermédiaire à afficher | Après la clôture, selon la configuration |
| `/topic/sessions/{codeAcces}/classement` | Enseignant | Classement complet (pseudonymes, scores, évolution) | Après la clôture de chaque question |
| `/user/queue/reponses` | Un étudiant | `{requestId, idQuestion, horodatageServeur}` | Réponse envoyée par STOMP et enregistrée |
| `/user/queue/erreurs` | Un étudiant | `{requestId, code, statut, error}` | Message STOMP refusé |
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
  "echeance": "2026-11-12T14:05:20+01:00"
}
```

Les dates (`echeance`, `horodatageServeur`) partent avec leur décalage horaire (`+01:00`), pour que le navigateur
les lise sans ambiguïté de fuseau. Le front calcule le temps restant à partir d'`echeance` et de l'heure du serveur (en-tête `Date` d'une réponse
REST), pour ne pas dépendre de l'horloge du téléphone.

## 5. Documentation interactive

À ajouter : `springdoc-openapi`, Swagger UI sur `/swagger-ui.html` et le schéma sur
`/v3/api-docs`, accessibles en développement et protégés en production.
