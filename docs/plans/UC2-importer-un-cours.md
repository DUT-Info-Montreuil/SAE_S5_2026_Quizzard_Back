# Plan d'implémentation – UC2 « Importer un cours » (back)

Périmètre : **back-end Spring Boot uniquement** (écran E2 « Mes cours », élément 3 « Importer un PDF »).
Spécification : dossier de conception, §4.1 (définition, cas d'erreur, diagrammes), §5.1.1 (services API) et §5.2.1
(services fonctionnels). Branche : `feature/uc2-importer-un-cours`, partie de `develop`.

Dans le périmètre : contrôle du PDF, stockage hors de l'espace public, création du cours et de sa version 1, doublon.

Hors périmètre : nouvelle version d'un cours (UC6), insertion de quiz (UC3), liste et lecture des cours,
connexion de l'enseignant (UC1).

## 1. État de départ

- `develop` : le socle seul. La feature `cours` est vide.
- Les branches des UC3, UC7, UC8 et UC9 ne sont pas fusionnées. L'UC2 ne part d'aucune d'elles.
- Le schéma `V1` a déjà `cours` et `version_cours` : aucune migration.

## 2. Décisions prises

| # | Décision |
|---|---|
| D1 | **Le dossier de conception fait foi** quand `docs/API.md` différait : le titre est obligatoire, `confirmerDoublon` permet d'importer un PDF déjà présent. |
| D2 | **Fichiers communs copiés à l'identique** : erreurs avec `code` et `400 REQUETE_INVALIDE` (même premier commit que les UC7 et UC8), `HorlogeConfig`, `EnseignantConnecte` (UC3). |
| D3 | **Classes propres à l'UC2 sous d'autres noms** que celles de l'UC3, qui a déjà `Cours`, `VersionCours` et `CoursRepository` : `CoursImporte`, `VersionImportee`, `ImportCoursRepository`, `PostgresImportCoursRepository`. |
| D4 | Titre invalide en `422` : le contrôleur ne l'annote pas `@Valid`, car le gestionnaire commun répond `400` à ces annotations. Le domaine le refuse, avant toute écriture de fichier. |
| D5 | Les imports d'un même enseignant passent l'un après l'autre (ligne verrouillée) : un double clic ne crée pas deux cours. |
| D6 | PDFBox 3.0.8 ; tout PDF chiffré est refusé, même s'il s'ouvre sans mot de passe. Ses logs sont coupés : ils recopient des fragments du fichier. |
| D7 | Fichiers rangés dans `STOCKAGE_DOSSIER` (par défaut `stockage/`, ignoré par Git), sous `cours/<UUID>.pdf`. |

## 3. Contrat

`POST /api/cours` (`multipart/form-data`), réservé à l'enseignant connecté, avec jeton CSRF.

| Champ | Contenu |
|---|---|
| `fichier` | Le PDF, obligatoire, 50 Mo au plus (à confirmer avec le client) |
| `titre` | 1 à 200 caractères, obligatoire |
| `confirmerDoublon` | Booléen facultatif, faux par défaut |

Succès : `201`, `Location: /api/cours/{idCours}`, corps `{idCours, titre, numeroVersion, nbPages, nbQuiz,
dateModification}`.

## 4. Tableau des erreurs

| Cas | Statut | `code` | Message |
|---|---|---|---|
| Fichier absent | `400` | — | « La requête est mal formée. » |
| Pas connecté | `401` | — | « Vous devez être connecté. » |
| Jeton CSRF absent | `403` | — | « Action non autorisée. » |
| Fichier trop gros | `413` | — | « Le fichier envoyé est trop volumineux. » |
| Titre absent, vide ou trop long | `422` | `TITRE_INVALIDE` | « Le titre est obligatoire (200 caractères au plus). » |
| Pas un PDF | `415` | `FICHIER_NON_PDF` | « Le fichier n'est pas un PDF. » |
| PDF corrompu, chiffré ou sans page | `422` | `PDF_ILLISIBLE` | « Le PDF est illisible ou protégé par un mot de passe. » |
| Doublon non confirmé | `409` | `DOUBLON_COURS` | « Vous avez déjà importé ce PDF dans le cours « … ». Importer quand même ? » |
| Écriture sur le disque ou en base en panne | `500` | — | Message générique ; rien n'est créé |

## 5. Machine à états d'un import

```text
reçu ──titre invalide──▶ refusé (rien n'est écrit)
reçu ──▶ fichier temporaire ──▶ analyse ──▶ doublon ? ──▶ déplacement ──▶ écriture en base ──▶ importé
                │ refus ou panne à n'importe quelle étape : rien n'est créé
                └─ dans tous les cas, le fichier temporaire est supprimé
transaction annulée après le déplacement ──▶ le fichier définitif est supprimé
```

## 6. Étapes

- [x] **B0 Préparation** : branche depuis `develop`, fichiers communs copiés, plan enregistré.
- [x] **B1 Socle** : `org.apache.pdfbox:pdfbox` 3.0.8 (accord du 25/11/2026), réglages de stockage et de taille.
- [x] **B2 Domaine `cours`** : `CoursImporte`, `VersionImportee`, exceptions.
- [x] **B3 Cas d'usage et ports** : `ImporterCoursUseCase`, `StockageFichiers`, `AnalyseurPdf`,
  `ImportCoursRepository`.
- [x] **B4 Adaptateurs** : `DisqueStockageFichiers`, `PdfBoxAnalyseurPdf`, PostgreSQL (JPA).
- [x] **B5 Web** : `CoursController`.
- [x] **B6 Démo** : profil `demo-uc2`.
- [x] **B7 Tests et vérification** : `./mvnw verify` sur la branche (73 tests), puis fusion simulée des UC9, UC8, UC3,
  UC7 et UC2 sur un clone jetable (388 tests, dont un parcours import → page de quiz → lancement → réponse écrit
  pour la simulation, non gardé).

### Essayer l'UC2 (profil `demo-uc2`)

Le profil crée l'enseignant de démonstration. À lancer avec `SECURITE_OUVERTE=true` dans `.env`, jamais sur le
serveur :

```bash
SECURITE_OUVERTE=true ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo-uc2
curl -F titre=Essai -F fichier=@mon-cours.pdf http://localhost:8080/api/cours
```

Une base de développement passée par les branches des UC3 ou UC7 (`V2`, `V3`) convient : Flyway ignore les
migrations plus récentes que celles de cette branche.

### Fusion avec les autres branches

L'UC2 n'ajoute aucun conflit. Les seuls conflits de la fusion simulée existent déjà entre l'UC3 et l'UC9
(`SecurityConfig`, `GestionnaireErreurs`, `TODO.md`) : garder la version de l'UC9 pour les deux fichiers Java.

## 7. Questions ouvertes

### Pour l'équipe back

| # | Sujet | Proposition |
|---|---|---|
| O1 | La réponse de l'import ne donne pas `idVersion`, dont l'UC3 a besoin pour insérer une page | La donner dans `GET /api/cours/{idCours}`, à écrire |
| O2 | Un connecté qui n'est pas enseignant reçoit `401` (`EnseignantConnecte`), le dossier prévoit `403` | À traiter avec l'UC1 (rôles) |

### Pour l'équipe front

| # | Sujet |
|---|---|
| F1 | Le titre est obligatoire (l'ancien `docs/API.md` le disait facultatif) : le proposer à partir du nom du fichier |
| F2 | Sur `409 DOUBLON_COURS`, renvoyer le même fichier avec `confirmerDoublon=true` |
| F3 | Contrôler la taille avant l'envoi : au-delà de 100 Mo, la connexion est coupée sans réponse |

### À valider avec le client

- Taille maximale de 50 Mo.
- Refus d'un PDF chiffré qui s'ouvre sans mot de passe (protection contre la copie ou l'impression).
- Refus d'un PDF précédé d'octets avant `%PDF-`.

### Écarts avec le dossier

- Le titre invalide est refusé par le domaine, pas par `@Valid` (qui répondrait `400`).
- Le message du `413` est celui du gestionnaire commun, pas « Le fichier dépasse la limite autorisée. »
- Une contrainte de base violée répond `409` (règle commune), pas `500`.
- Le message du doublon cite le titre du cours existant, comme le demande le dossier : le gestionnaire commun l'écrit
  aussi dans les logs.

### Pas encore fait

- Liste et détail des cours, lecture du PDF, nouvelle version (UC6).
- Suppression des fichiers orphelins (UC20).

## 8. Critères d'acceptation

- [x] L'import crée le cours, sa version 1 et le fichier, ou rien.
- [x] Chaque cas d'erreur du §4 renvoie son statut et son message (la panne de la base est simulée, pas provoquée).
- [x] Le fichier est hors de l'espace public et ne reste pas en double.
- [x] Ni titre, ni nom de fichier, ni contenu de fichier dans les logs d'un import réussi.
- [x] `./mvnw verify` passe, sur la branche et après fusion simulée.
