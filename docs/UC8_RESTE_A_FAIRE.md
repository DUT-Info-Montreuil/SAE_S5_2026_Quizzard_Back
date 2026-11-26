# UC8 « Rejoindre un quiz » : ce qui reste autour

Fichier séparé de `TODO.md` pour éviter un conflit : les branches UC3 et UC9 y écrivent au même endroit. À reporter
dans `TODO.md` une fois toutes les branches fusionnées.

## À faire à la fusion

- Retirer de `TODO.md` la ligne « UC8 : créer le participant et son jeton » (ajoutée par l'UC9), qui est faite.
- Regrouper `DiffuseurConnectes` avec `DiffuseurTempsReel` (UC9), et `EntreeParticipantRepository` avec
  `ParticipantRepository` : séparés ici pour que les branches fusionnent sans conflit.

## Reste à faire

- **M** Contrôle du jeton à la connexion STOMP et des abonnements : apporté par l'UC9. **Tant que l'UC9 n'est pas
  fusionnée, `/ws` accepte toute connexion** (configuration partagée reprise de l'UC9) : ne pas déployer cette branche
  seule.
- **M** `GET /api/sessions/{idSession}` : relire le nombre de connectés après un rechargement de l'écran E5.
- **S** Limite du nombre d'entrées par appareil (cahier des charges §3.2.2.4) : seuil à fixer avec le client.
- **S** Reprise après une coupure réseau (`POST /api/public/sessions/{codeAcces}/reconnexion`) : relire le jeton par
  `JetonParticipant.empreinte`.
- **S** Quiz noté : `POST /api/public/sessions/{codeAcces}/participants/ldap` (UC15), avec le pseudonyme d'affichage.
- **C** Compteur de connectés = nombre de participants ; le départ d'un participant (`derniere_activite`) reste à
  gérer.
- À valider avec le client : pseudonymes sensibles à la casse (`Alice` ≠ `alice`), caractères de mise en forme
  invisibles (zero-width, inversion de sens d'écriture) dans les pseudonymes.
- Le chemin des requêtes (`path` de `request_handled`) et le `toString()` de `SessionQuiz` contiennent le code d'accès,
  secret tant que la séance dure : décider avec l'équipe s'il faut le masquer dans les logs.
