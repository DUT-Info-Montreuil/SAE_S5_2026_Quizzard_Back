# CLAUDE.md

Consignes pour Claude Code (et tout assistant de code) qui travaille dans ce dépôt.

## Projet

Back-end de **Quizzard**, outil de quiz dynamique de l'IUT de Montreuil (SAÉ 5.01A, BUT 3, 2026-2027).
Un enseignant importe un cours PDF, y intercale des pages de quiz et de classement, puis anime les quiz en direct ;
les étudiants répondent depuis leur téléphone (QR code ou lien), sans compte ou avec leur compte LDAP pour un quiz noté.
Le front React est dans un dépôt séparé.

Pile : **Java 25, Spring Boot 4.1** (Web MVC, Security, Data JPA, WebSocket, Validation, Actuator), **PostgreSQL 18**,
**Flyway**, Jackson 3 (`tools.jackson`), Testcontainers. Build avec le wrapper Maven.

Documentation à lire avant de modifier une zone :

- `docs/ARCHITECTURE.md` : paquets, couches, flux, sécurité, temps réel.
- `docs/DESIGN.md` : règles métier (scores, séance, réponses libres, réapplication des quiz) et conventions de code.
- `docs/API.md` : routes REST et destinations STOMP.
- `docs/DATABASE.md` : schéma et règles Flyway.
- `CONTRIBUTING.md` : branches, commits, pull requests.
- `TODO.md` : ce qui reste à faire.

## Commandes

```bash
./mvnw spring-boot:run
```

Démarre l'API sur le port 8080 ; PostgreSQL est lancé automatiquement par `spring-boot-docker-compose` (port 5433).

```bash
./mvnw test
```

Tous les tests ; Docker doit tourner (Testcontainers). Un seul test : `./mvnw test -Dtest=SocleWebTest`.

```bash
./mvnw verify
```

Vérification complète avant une pull request.

## Architecture en bref

```text
fr.iutmontreuil.quizzard
├── features/<feature>/{domain, application/{port,dto}, infrastructure/{web,persistance,…}}
└── shared/{domain, application/port, infrastructure/{journalisation,securite,web,…}}
```

Features : `compte`, `cours`, `quiz`, `seance`, `classement`, `resultat`, `administration`.

Règles à respecter :

1. **Dépendances** : `domain` ne dépend ni de Spring ni de JPA ; `application` dépend de `domain` et déclare des ports ;
   `infrastructure` implémente les ports. Une feature n'importe jamais l'`infrastructure` d'une autre feature.
2. **Contrôleurs minces** : validation de forme (`@Valid`) puis appel d'un cas d'usage ; pas de règle métier.
3. **Exceptions** : créer une exception nommée qui hérite d'une catégorie de `shared.domain`
   (`ExceptionIntrouvable` 404, `ExceptionConflit` 409, `ExceptionAccesRefuse` 403, `ExceptionNonAuthentifie` 401,
   `ExceptionTypeNonSupporte` 415, `ExceptionDonneesInvalides` 422). Ne jamais ajouter de correspondance
   exception → statut ailleurs que dans `GestionnaireErreurs`.
4. **Journalisation** : passer par le port `JournalEvenements` ; `actionSensible` pour connexion, import, suppression,
   archivage, lancement de séance, correction, export. **Aucune donnée personnelle** (nom, pseudonyme, e-mail,
   texte de réponse, mot de passe) dans les logs ni dans les messages d'erreur.
5. **Base** : le schéma ne change que par une **nouvelle** migration Flyway `V<n>__description.sql`. Ne jamais modifier
   une migration existante. Hibernate est en `validate`.
6. **Intégrité du jeu** : l'horloge du serveur fait foi ; une réponse par participant et par question ; les réponses
   attendues (`estCorrecte`, paires, motifs) ne sont jamais envoyées aux étudiants avant la clôture de la question.
7. **Propriétaire** : chaque cas d'usage vérifie que la ressource appartient à l'enseignant connecté.
8. **Motifs regex** saisis par les enseignants : longueur bornée et moteur à temps linéaire (voir `docs/DESIGN.md` §5).
9. Pas de dépendance externe à l'exécution (CDN, API tierce) : hébergement interne à l'IUT.

## Conventions

- Code métier, commentaires, messages d'erreur et documentation **en français** ; termes techniques imposés en anglais
  (`Controller`, `Repository`, `Config`).
- Injection par constructeur ; visibilité paquet pour les configurations et adaptateurs quand c'est possible.
- Noms de tests en `snake_case` décrivant le comportement.
- JSON en `camelCase` ; le format d'erreur garde `error`, `request_id`, `champs`.
- Mettre à jour `docs/API.md` / `docs/DATABASE.md` dans la même modification qu'une route ou une table.

## Git et livraison

- **Avant tout commit, push ou pull request : présenter les changements à l'utilisateur et attendre sa validation.**
- Branches `TYPE/description-avec-tirets` (`FEAT/`, `FIX/`, `DOCS/`, `TEST/`, `REFACTOR/`, `CHORE/`), créées depuis
  `develop`, pull request vers `develop`.
- Messages de commit en français, première ligne de 72 caractères au plus.
- Ne jamais commiter `.env`, un secret, un PDF de cours réel ou des données d'étudiants.

## Points d'attention actuels

- `SECURITE_OUVERTE=true` (fichier `.env`) ouvre toutes les routes en développement ; à supprimer quand la feature
  `compte` sera faite. Ne jamais l'activer par défaut.
- L'authentification LDAP arrive dans un second temps : comptes locaux (BCrypt) d'abord, derrière un port.
- Plusieurs règles sont « à valider » avec le client (voir `docs/DESIGN.md`) : ne pas les
  présenter comme définitives.
