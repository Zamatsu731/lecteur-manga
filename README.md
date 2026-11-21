# Lecteur Manga

Application Android personnelle pour lire des mangas et des comics, avec une interface en français.

## Télécharger

L'APK **3.8** est disponible dans [releases/Lecteur-Manga-3.8.apk](releases/Lecteur-Manga-3.8.apk).

Android 8.0 ou supérieur. Pour remplacer une ancienne version fournie dans la conversation, installer cet APK par-dessus, sans désinstaller, pour conserver les données.

## Fonctions

- Recherche, catégories et tri des titres.
- Onglet Catégories : recherche sans accents, sélection jusqu’à 5 genres, genres récents et listes propres à chaque catalogue.
- Catalogue de mangas et de webtoons, catalogue complémentaire de mangas et de comics.
- Favoris, historique et reprise à la page enregistrée.
- Lecture verticale ou page par page ; thème clair ou sombre.
- Zoom natif à deux doigts, jusqu'à 5×.
- Téléchargement par chapitre, progression, pause et reprise.
- Lecture hors connexion des chapitres entièrement téléchargés et gestion de l'espace occupé.
- Interface de lecture sans publicité et sans compte obligatoire.

## Compiler

Ouvrir le projet dans Android Studio, avec **JDK 17**, **Android SDK 35** et **Gradle 8.9**. Le projet utilise Android Gradle Plugin 8.7.3 et jsoup 1.18.3.

Le wrapper Gradle n'est pas inclus. Avec Gradle 8.9 installé :

```sh
gradle :app:assembleDebug
```

L'APK de développement sera créé dans `app/build/outputs/apk/debug/`.

La clé de signature utilisée pour l'APK fourni n'est pas dans ce dépôt. Une compilation personnelle utilise une autre clé : elle ne pourra donc pas remplacer directement cet APK signé. Utiliser un appareil de test ou une autre identité d'application pour éviter de perdre ses données.

## Structure

- `app/src/` : application Android, catalogues, classement et téléchargements.
- `app/assets/` : interface du lecteur en HTML, CSS et JavaScript.
- `app/res/` : icône et ressources Android.
- `tests/` : tests autonomes du classement Anime-Comics/Comics et de la pagination filtrée.
- `releases/` : APK signé prêt à installer.

## Limites et validation

Les catalogues dépendent de sites tiers : leur disponibilité, leurs protections et leur présentation peuvent changer. Les données ne sont pas hébergées par cette application.

Le téléchargement doit se faire avec l'application ouverte. Une interruption peut être reprise à la prochaine ouverture. Les chapitres sont enregistrés dans l'espace privé de l'application ; désinstaller celle-ci les efface.

La compilation et la signature de l'APK ont été vérifiées. Des tests de parsing, de classement, de pagination et de navigation simulée passent. L'ensemble des lecteurs et le zoom doivent encore être vérifiés sur un appareil Android réel. Le catalogue SushiScan n'a pas pu être validé directement depuis l'environnement de développement.

Projet non officiel, sans affiliation avec les sites référencés. Aucun chapitre ni image de lecture n'est inclus dans le dépôt.

## Tester l’interface

Avec Node.js et npm installés :

```sh
npm install
npm run test:ui
```

Ces tests simulent les catalogues : ils vérifient la navigation et les filtres, sans valider la disponibilité des sites tiers.
