# TODO – back-end Quizzard

Reste à faire côté back-end, classé par priorité MoSCoW du cahier des charges. Le détail des tickets vit dans Jira ;
ce fichier donne la vue d'ensemble et se met à jour à chaque Sprint Review.

## Fait

- **UC9 « Répondre à une question »** (QUIZZARD-33) : `POST /api/public/sessions/{codeAcces}/reponses`,
  `GET .../etat`, envoi STOMP `/app/sessions/{idSession}/reponses`, contrôle du jeton à la connexion STOMP,
  répartition en direct vers l'enseignant. Contrat dans [docs/API.md](docs/API.md) §3.5 et §4.

## MUST

- **UC7 « Lancer un quiz »** : ouvrir et clore les questions, diffuser `/question`, `/etat`, `/correction`,
  `/user/queue/resultat`. Écrire `position_courante` comme le lit l'UC9 (rang de la page de quiz dans la version,
  voir docs/API.md §3.5) ou adapter `PostgresSessionQuizRepository`.
- **UC8 « Rejoindre un quiz »** : créer le participant et son jeton (seule l'empreinte SHA-256 est stockée).
- `POST /api/public/sessions/{codeAcces}/reconnexion` : forme de la réponse à définir avec le front.
- Connexion STOMP de l'enseignant (feature `compte`) : aujourd'hui possible seulement avec `SECURITE_OUVERTE=true`.
- Supprimer le profil `demo` quand l'UC7 et l'UC8 seront faits.

## Essayer l'UC9 sans l'UC7 ni l'UC8 (profil `demo`)

Le profil crée au démarrage la séance `DEMO` (QCM, points à relier, réponse libre) et ouvre la première question.
Les routes de démonstration n'ont ni connexion ni CSRF : à lancer avec `SECURITE_OUVERTE=true`, jamais sur le serveur.

```bash
SECURITE_OUVERTE=true ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

- Jeton du participant : `jeton-demo`. Côté front, le déposer dans `sessionStorage` sous la clé
  `quizzard.jeton.DEMO`, puis ouvrir `http://localhost:5173/rejoindre/DEMO/question`.
- Ouvrir la question n (1 à 3) : `POST /api/demo/sessions/DEMO/questions/{n}` ; clore : `POST /api/demo/sessions/DEMO/cloture`.
