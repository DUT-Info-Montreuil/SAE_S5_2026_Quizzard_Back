# TODO – back-end Quizzard

Reste à faire côté back-end, classé par priorité MoSCoW du cahier des charges. Le détail des tickets vit dans Jira ;
ce fichier donne la vue d'ensemble et se met à jour à chaque Sprint Review.

## Fait

- **UC3 « Insérer une page de quiz dans le cours »** (QUIZZARD-39) : `POST /api/cours/{idCours}/pages-quiz`,
  migration V2 (position des pages différée). Contrat dans [docs/API.md](docs/API.md) §3.2.
- **Éditeur E3, pages insérées** : `GET /api/cours/{idCours}/versions/{idVersion}/pages`, `DELETE` et `PATCH`
  `/api/pages/{idPage}` (retirer, déplacer), `GET .../versions/{idVersion}/fichier` (PDF pour PDF.js).

## MUST

- **UC1 « Se connecter »** (feature `compte`) : remplir le contexte Spring Security avec l'identifiant LDAP de
  l'enseignant, lu par `EnseignantConnecte`. En attendant, les tests simulent l'enseignant (`@WithMockUser`).
- **UC2 « Importer un cours »** : crée le cours et ses versions dont l'UC3 a besoin.
- PDF d'une version pour les étudiants (affichage du cours en séance) : la lecture enseignant est faite.
- Rapprocher `LectureFichiersCours` (UC3, lecture) et `StockageFichiers` (UC2, écriture) au merge de l'UC2.
