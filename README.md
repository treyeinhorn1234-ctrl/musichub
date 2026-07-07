# MusicHub

Une app Android qui réunit **Spotify, Deezer, YouTube et SoundCloud** derrière une
seule recherche. Tu tapes un titre ou un artiste, tu choisis le service, et l'app
ouvre le résultat directement dans l'application officielle correspondante (ou dans
le navigateur si elle n'est pas installée).

L'app ne lit **aucun** flux audio elle-même : la lecture reste à l'intérieur de
chaque service officiel, ce qui respecte leurs conditions d'utilisation et ton
abonnement. Pas de compte à créer, pas de connexion — c'est un lanceur intelligent.

## Fonctions

- Recherche universelle → Spotify / Deezer / YouTube / SoundCloud
- Ouverture dans l'app native via deep link (fallback navigateur)
- Historique des recherches récentes (stocké localement sur le téléphone)
- Thème clair/sombre automatique, icône dédiée

---

## Obtenir l'APK compilé automatiquement (recommandé)

Aucun logiciel à installer sur ton ordinateur. GitHub compile l'app pour toi.

1. Crée un dépôt **GitHub** vide (bouton *New repository*).
2. Envoie-y le contenu de ce dossier. Le plus simple sans ligne de commande :
   sur la page du dépôt, clique *Add file → Upload files*, glisse **tout le contenu**
   du dossier `MusicHub` (pas le dossier lui-même, son contenu), puis *Commit*.
3. Va dans l'onglet **Actions** du dépôt : le workflow « Build APK » se lance seul
   (sinon clique dessus puis *Run workflow*).
4. Après ~3 minutes, deux façons de récupérer l'APK :
   - **Releases** (à droite de la page d'accueil du dépôt) → **latest** → télécharge
     `MusicHub.apk`. C'est le plus pratique depuis ton téléphone.
   - ou onglet *Actions* → dernier run → section *Artifacts* → `MusicHub-apk`.

Puis sur le téléphone : ouvre `MusicHub.apk`, autorise « installer des applications
inconnues » pour ta source (navigateur / gestionnaire de fichiers), et installe.

## Compiler en local (alternative)

Avec **Android Studio** : *File → Open* → sélectionne le dossier `MusicHub`, laisse
Gradle se synchroniser, puis *Build → Build Bundle(s) / APK(s) → Build APK(s)*.

En ligne de commande (SDK Android installé, variable `ANDROID_HOME` définie) :

```bash
./gradlew assembleDebug
# APK généré : app/build/outputs/apk/debug/app-debug.apk
```

## Détails techniques

- Kotlin, vues Android classiques + Material 3
- `minSdk 24` (Android 7.0), `targetSdk 34`
- AGP 8.5.2, Gradle 8.7, JDK 17
- Aucune dépendance réseau au runtime, aucune permission spéciale

## Personnaliser

- Couleurs / nom des services : `app/src/main/java/com/trey/musichub/MainActivity.kt`
  (la liste `services`).
- Nom de l'app et textes : `app/src/main/res/values/strings.xml`.
- Icône : `app/src/main/res/mipmap-*` et `drawable/ic_launcher_foreground.xml`.
