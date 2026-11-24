# TODO – back-end Quizzard

Reste à faire côté back-end, classé par priorité MoSCoW du cahier des charges. Le détail des tickets vit dans Jira ;
ce fichier donne la vue d'ensemble et se met à jour à chaque Sprint Review.

## Fait

- **UC3 « Insérer une page de quiz dans le cours »** (QUIZZARD-39) : `POST /api/cours/{idCours}/pages-quiz`,
  migration V2 (position des pages différée). Contrat dans [docs/API.md](docs/API.md) §3.2.

## MUST

- **UC1 « Se connecter »** (feature `compte`) : remplir le contexte Spring Security avec l'identifiant LDAP de
  l'enseignant, lu par `EnseignantConnecte`. En attendant, les tests simulent l'enseignant (`@WithMockUser`).
- **UC2 « Importer un cours »** : crée le cours et ses versions dont l'UC3 a besoin.
