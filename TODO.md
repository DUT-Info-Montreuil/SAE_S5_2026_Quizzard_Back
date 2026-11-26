# TODO – back-end Quizzard

Reste à faire côté back-end, classé par priorité MoSCoW du cahier des charges. Le détail des tickets vit dans Jira ;
ce fichier donne la vue d'ensemble et se met à jour à chaque Sprint Review.

## Fait

- **UC9 « Répondre à une question »** (QUIZZARD-33) : `POST /api/public/sessions/{codeAcces}/reponses`,
  `GET .../etat`, envoi STOMP `/app/sessions/{idSession}/reponses`, contrôle du jeton à la connexion STOMP,
  répartition en direct vers l'enseignant. Contrat dans [docs/API.md](docs/API.md) §3.5 et §4.
- **UC3 « Insérer une page de quiz dans le cours »** (QUIZZARD-39) : `POST /api/cours/{idCours}/pages-quiz`,
  migration V2 (position des pages différée). Contrat dans [docs/API.md](docs/API.md) §3.2.
- **Éditeur E3, pages insérées** : `GET /api/cours/{idCours}/versions/{idVersion}/pages`, `DELETE` et `PATCH`
  `/api/pages/{idPage}` (retirer, déplacer), `GET .../versions/{idVersion}/fichier` (PDF pour PDF.js).

## MUST

- **UC1 « Se connecter »** (feature `compte`) : remplir le contexte Spring Security avec l'identifiant LDAP de
  l'enseignant, lu par `EnseignantConnecte`. En attendant, les tests simulent l'enseignant (`@WithMockUser`).
- `POST /api/public/sessions/{codeAcces}/reconnexion` : forme de la réponse à définir avec le front.
- Connexion STOMP de l'enseignant (feature `compte`) : aujourd'hui possible seulement avec `SECURITE_OUVERTE=true`.
- Supprimer le profil `demo` : l'UC7 et l'UC8 sont fusionnées, il double maintenant l'UC7.
- PDF d'une version pour les étudiants (affichage du cours en séance) : la lecture enseignant est faite.
- Rapprocher `LectureFichiersCours` (UC3, lecture) et `StockageFichiers` (UC2, écriture) : l'UC2 est fusionnée.

## Essayer l'UC9 sans l'UC7 ni l'UC8 (profil `demo`)

Le profil crée au démarrage la séance `DEMO` (QCM, points à relier, réponse libre) et ouvre la première question.
Les routes de démonstration n'ont ni connexion ni CSRF : à lancer avec `SECURITE_OUVERTE=true`, jamais sur le serveur.

```bash
SECURITE_OUVERTE=true ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

- Jeton du participant : `jeton-demo`. Côté front, le déposer dans `sessionStorage` sous la clé
  `quizzard.jeton.DEMO`, puis ouvrir `http://localhost:5173/rejoindre/DEMO/question`.
- Ouvrir la question n (1 à 3) : `POST /api/demo/sessions/DEMO/questions/{n}` ; clore : `POST /api/demo/sessions/DEMO/cloture`.
