# Architecture d'extension pédagogique

Le dépôt sépare les points d'extension des parties publiées afin que les
apprenants puissent modifier leur clone et récupérer de nouveaux modules sans
réécriture systématique des fichiers existants.

## Événements

- Les contrats versionnés sont placés dans `contracts/events`.
- Un contrat publié est immuable ; une évolution crée une nouvelle version.
- Le producteur publie uniquement dans `events:simple:published`.
- `eventRouterService` charge les fichiers de `contracts/subscriptions`.
- Un nouveau consommateur ajoute son propre descripteur et sa propre file.

Le routeur utilise `LMOVE` vers `events:simple:routing`. Une interruption laisse
donc l'événement récupérable. Un crash après certaines publications peut créer
des doublons ; chaque consommateur doit rester idempotent.

## Angular

La page de validation expose un contexte stable à une liste de composants
d'extension. Chaque extension se trouve sous `src/app/extensions/<feature>` et
possède son composant, ses modèles, son client HTTP et `extension.json`.

Le script Python `car-rental-angular/scripts/generate_extensions.py` produit le
registre TypeScript avant `ng serve` ou `ng build`. Ce fichier est généré et
ignoré par Git. Ajouter
une extension ne modifie donc ni la page hôte ni les extensions publiées.

## Formation

Les sources Quarto sont maintenues dans deux dépôts séparés :
[`NoSQL`](https://github.com/charroux/NoSQL) et
[`EventDrivenArchitecture`](https://github.com/charroux/EventDrivenArchitecture).
Chaque dépôt enregistre dans `case-study.yml` la révision compatible de
`rentalservice`. Les decks, les laboratoires et le code pédagogique peuvent
ainsi évoluer sans modifier le dépôt applicatif.

Le gel par empreinte demeure disponible dans `tools/training-ci`, mais aucun
manifeste n'est actif. Il pourra être transféré dans les dépôts de formation
après validation explicite de chaque module en situation de formation.
