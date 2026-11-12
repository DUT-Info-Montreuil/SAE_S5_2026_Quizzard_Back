# Contribuer au back-end Quizzard

Guide de travail de l'équipe pour le dépôt back-end (SAÉ 5.01A, BUT 3, 2026-2027). Il vaut pour les cinq membres,
quel que soit leur rôle du sprint (Dev Front, Dev Back, Dev/Ops – Dev qualité).

À lire aussi : [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/DESIGN.md](docs/DESIGN.md) (conventions de code),
[docs/API.md](docs/API.md), [docs/DATABASE.md](docs/DATABASE.md), [TODO.md](TODO.md).

---

## 1. Prérequis

| Outil | Version | Remarque |
|---|---|---|
| JDK | **25** | `java -version` doit afficher 25 |
| Docker + Docker Compose | Récent | Base de développement et tests (Testcontainers) |
| Maven | Inutile | Utiliser le wrapper `./mvnw` (Maven 3.9.16) |
| IDE | IntelliJ IDEA conseillé | Encodage UTF-8, fins de ligne gérées par `.gitattributes` |

## 2. Démarrer en local

```bash
git clone https://github.com/DUT-Info-Montreuil/SAE_S5_2026_Quizzard_Back.git
```

```bash
cd SAE_S5_2026_Quizzard_Back
```

```bash
cp .env.example .env
```

Le fichier `.env` est facultatif : sans lui, les valeurs par défaut de `compose.yaml` s'appliquent. Il n'est jamais
commité.

```bash
./mvnw spring-boot:run
```

Spring Boot démarre lui-même PostgreSQL avec Docker Compose (port `5433`), Flyway crée le schéma, l'API répond sur
`http://localhost:8080`. Vérification :

```bash
curl http://localhost:8080/actuator/health
```

Tant que la connexion n'existe pas, mettre `SECURITE_OUVERTE=true` dans `.env` pour appeler l'API depuis Postman ou
le front. **Jamais sur le serveur.**

## 3. Tests

```bash
./mvnw test
```

Les tests d'intégration démarrent un PostgreSQL 18 avec Testcontainers : Docker doit tourner. Avant d'ouvrir une pull
request, lancer la vérification complète :

```bash
./mvnw verify
```

Attendu pour toute nouvelle fonctionnalité :

- tests unitaires du domaine (règles, calculs, états) ;
- test du cas d'usage (droits du propriétaire, cas d'erreur) ;
- test du contrôleur avec MockMvc (statuts, format d'erreur, sécurité) ;
- un nom de test qui décrit le comportement : `une_reponse_hors_delai_est_refusee`.

## 4. Branches

| Branche | Rôle | Qui y pousse |
|---|---|---|
| `main` | Versions livrées et taguées (`1.0.0` le 16/12/2026, `2.0.0` le 15/01/2027) | Fusion de `develop` en fin de sprint uniquement |
| `develop` | Intégration continue de l'équipe | Pull requests relues uniquement |
| `TYPE/description` | Travail en cours | Son auteur |

Nommage des branches de travail : un préfixe en majuscules, puis une description courte **avec des tirets** (Git
refuse les espaces).

| Préfixe | Usage | Exemple |
|---|---|---|
| `FEAT/` | Nouvelle fonctionnalité | `FEAT/import-pdf` |
| `FIX/` | Correction d'anomalie | `FIX/reponse-hors-delai` |
| `DOCS/` | Documentation seule | `DOCS/api-classements` |
| `TEST/` | Tests seuls | `TEST/charge-300-participants` |
| `REFACTOR/` | Restructuration sans changement de comportement | `REFACTOR/ports-seance` |
| `CHORE/` | Outillage, dépendances, CI, Docker | `CHORE/ci-github-actions` |

Une branche part de `develop` à jour et ne traite qu'un ticket Jira.

```bash
git switch develop
```

```bash
git pull
```

```bash
git switch -c FEAT/import-pdf
```

## 5. Commits

- Messages **en français**, courts et explicites : ce que fait le commit, pas comment.
- Première ligne de 72 caractères au plus ; détails éventuels après une ligne vide.
- Un commit = un changement cohérent qui compile et passe les tests.

```text
Import d'un PDF : contrôle du type et de la taille

Refuse les fichiers non PDF (415) et au-delà de 50 Mo (413).
Empreinte SHA-256 pour détecter un PDF déjà importé (409).
```

## 6. Pull requests

1. Pousser la branche, ouvrir une pull request vers **`develop`**.
2. Titre clair, description : ticket Jira, UC concerné, ce qui change, comment tester, captures ou exemples de
   requêtes si utile.
3. Au moins **une relecture approuvée** par un autre membre ; le Dev qualité du sprint relit en priorité.
4. Les tests passent (`./mvnw verify`).
5. Fusion par l'auteur après approbation, puis suppression de la branche.

Checklist du relecteur :

- [ ] Le code respecte la règle de dépendance (domaine → rien ; application → domaine ; infrastructure → les deux).
- [ ] Les règles métier sont dans le domaine ou le cas d'usage, pas dans le contrôleur.
- [ ] Le propriétaire de la ressource est vérifié.
- [ ] Les exceptions héritent d'une catégorie de `shared.domain` ; aucune nouvelle correspondance HTTP ailleurs.
- [ ] Aucune donnée personnelle dans les logs ni dans les messages d'erreur.
- [ ] Les réponses attendues ne sont jamais envoyées aux étudiants avant la clôture.
- [ ] Toute modification du schéma passe par une **nouvelle** migration Flyway.
- [ ] `docs/API.md` et `docs/DATABASE.md` sont à jour si une route ou une table change.
- [ ] Des tests couvrent le cas nominal et les cas d'erreur.

## 7. Définition de « terminé »

Un ticket est terminé quand :

- le code est fusionné dans `develop` après relecture ;
- les tests automatiques passent ;
- la documentation concernée est à jour (API, base, conception) ;
- le comportement est vérifié contre le cas d'utilisation du cahier des charges ;
- le scénario est ajouté au cahier de recette s'il est visible par l'utilisateur.

## 8. Cérémonies et outils

- **Jira** : backlog et tickets ; un ticket = une branche = une pull request.
- **Refinement, Sprint Planning, Sprint Review** à chaque sprint ; Sprint Review du Sprint 3 en anglais.
- **Rotation des rôles** à chaque sprint : la personne qui reprend une feature doit pouvoir la comprendre avec le code,
  les tests et ces documents.
- **Moodle** : dépôt des livrables.

## 9. Fin de sprint et versions

1. Geler `develop`, vérifier les tests et le cahier de recette.
2. Fusionner `develop` dans `main` par pull request.
3. Taguer `main` (`v1.0.0`, `v2.0.0`, ou `sprint-N` pour les sprints intermédiaires).
4. Mettre à jour [TODO.md](TODO.md).
