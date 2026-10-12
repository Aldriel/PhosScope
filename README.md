# PhosScope

Outil de phosphoprotéomique en Java 21 qui permet de :

- lire un tableau quantitatif **MaxQuant** (`Phospho (STY)Sites.txt`), **FragPipe** (colonnes
  `Intensity` ou `MaxLFQ Intensity`) ou un tableau délimité générique ;
- **nettoyer** les lignes (contaminants, leurres inversés, peptides identifiés seulement par une
  modification, probabilité de localisation) ;
- **imputer** les valeurs manquantes, surtout par le tirage normal décalé de Perseus ;
- comparer un groupe **témoin** et un groupe **malade** par un test t de Welch et un ajustement
  de Benjamini–Hochberg ;
- produire un **rapport PDF** et un tableau des sites, en français ou en anglais.

Le même modèle est servi par deux applications indépendantes, toutes deux en architecture MVC :
une application **desktop JavaFX** et une application **web React**, prévue pour
[phosscope.maximeethier.com](https://phosscope.maximeethier.com).

## Données d'exemple

L'exemple « tumeur simulée » est **inventé par PhosScope** (graine fixe). Quelques sites ont un
effet planté (AKT1 et MAPK1 à la hausse, EGFR et PTEN à la baisse), d'autres non, et le fichier
contient aussi les lignes qu'un vrai export demande de retirer. Ce ne sont pas des données de
patients.

## Organisation du dépôt

```
core/       Modèle partagé (Maven, phosscope-core) : lecture, nettoyage, imputation,
            statistiques, exemple, rapport PDF et façade PhosService
desktop/    Application desktop (Maven, phosscope-desktop) : vue JavaFX, contrôleur, aide, i18n
server/     API REST (Maven, phosscope-server) : espaces de travail en mémoire
web/        Application web (npm) : React + TypeScript + Vite
deploy/     Installation sur un serveur Ubuntu (systemd + Caddy)
config/     Règles Checkstyle communes aux modules Java
```

### Architecture MVC et patrons de conception

| Couche | Desktop | Web |
|---|---|---|
| Modèle | `core` (`PhosService`) + `DesktopModel` | `core` via l'API REST ; `web/src/model` |
| Vue | `MainView` (JavaFX, sans calcul) | `web/src/views` |
| Contrôleur | `MainController` | `ApiHandler` et les actions de `App` |

- **Façade** : `PhosService` est le seul point d'entrée vers le modèle.
- **Stratégie** : `ImputationMethod` choisit Perseus, la moitié du minimum, ou aucune imputation.
- **Monteur** : `AnalysisSettings.Builder` assemble les réglages.
- **Objets immuables** : les sites, les réglages et le résultat sont des `record`.

## Application desktop

- Cinq onglets : **Données**, **Nettoyage**, **Imputation**, **Résultats**, **Rapport**.
- Interface **bilingue** et **taille du texte** réglable (Ctrl + / Ctrl − / Ctrl 0), mémorisées.
- Menu **Aide** : guide d'utilisateur.

```powershell
.\mvnw -pl desktop javafx:run
```

L'archive Windows autonome se construit avec `desktop/package-windows.ps1` (JDK 21, `jlink` et
`jpackage`). Elle produit `desktop/target/dist/PhosScope-0.1.0-windows-x64.zip`.

## Application web

Les mêmes calculs et les mêmes textes. Les fichiers restent **en mémoire** 30 minutes, jamais sur
disque (40 Mo au plus). Alt n'est pas requis : les onglets sont des boutons.

```powershell
.\mvnw -pl server exec:java              # API sur http://127.0.0.1:7073/api
cd web; npm ci; npm run dev              # http://localhost:5176
```

## Qualité du code

- Aucune ligne ne dépasse 100 colonnes. Java formaté en indentation de 4 espaces, TypeScript avec
  Prettier.
- Checkstyle, ESLint et l'intégration continue GitHub (`.github/workflows/ci.yml`).
- JaCoCo et Vitest exigent **95 % des lignes** et **90 % des branches**.
- Une vérification quotidienne (`.github/workflows/monitor.yml`) rejoue le parcours d'un visiteur
  sur le site en ligne, en français et en anglais.

```powershell
.\mvnw verify
cd web; npm run check
npm run test:e2e:local
```

## Déploiement

Le site est prévu sur le serveur Ubuntu déjà utilisé par les autres applications, avec un tas Java
de **384 Mo** (les tableaux sont bien plus légers que des fichiers FCS). Après `npm run build` :

```bash
bash setup-server.sh phosscope.maximeethier.com
```

Il faut un enregistrement DNS `phosscope.maximeethier.com` vers le même droplet. Un droplet plus
gros n'est nécessaire que si `free -h` montre moins d'environ 500 Mo disponibles une fois les
autres applications démarrées.

## Licence

Copyright 2026 Maxime Ethier - Consultant en Bioinformatique/Biocomputing Consultant.

Ce projet est distribué sous la licence Apache 2.0 : voir le fichier [`LICENSE`](LICENSE).
