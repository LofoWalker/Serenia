# Étape 8 : Analyse du Code Mort et Dépendances Inutiles

> **Priorité** : 🟢 Moyenne | **Bloquant** : Non

## Objectif

Nettoyer le repository de tout code non utilisé et des dépendances superflues pour faciliter la maintenance par la communauté.

## Actions à Exécuter

### 1. Détection de code mort - Backend (Java)

#### Avec SpotBugs

```bash
cd backend
./mvnw spotbugs:check
```

#### Avec IntelliJ IDEA (manuel)
- `Analyze > Run Inspection by Name > Unused declaration`
- `Analyze > Run Inspection by Name > Unused import`

#### Rechercher les imports inutilisés

```bash
# Les IDE modernes le font automatiquement, mais pour vérifier :
grep -rn "^import " --include="*.java" backend/src/main/java/ | wc -l
```

### 2. Détection de code mort - Frontend (TypeScript)

#### Avec ts-prune

```bash
cd frontend
npx ts-prune
```

#### Avec ESLint

```bash
npx eslint --ext .ts src/ --rule 'no-unused-vars: error'
```

### 3. Analyse des dépendances inutiles - Backend

```bash
cd backend
./mvnw dependency:analyze
```

Sortie attendue :
- `Unused declared dependencies` : À supprimer du pom.xml
- `Used undeclared dependencies` : À déclarer explicitement

### 4. Analyse des dépendances inutiles - Frontend

```bash
cd frontend
npx depcheck
```

Sortie attendue :
- `Unused dependencies` : À supprimer du package.json
- `Missing dependencies` : À ajouter

### 5. Rechercher les fichiers orphelins

```bash
# Fichiers TypeScript non importés
find frontend/src -name "*.ts" -type f | while read f; do
  basename=$(basename "$f" .ts)
  if ! grep -rq "$basename" frontend/src --include="*.ts" --include="*.html"; then
    echo "Potentiellement orphelin: $f"
  fi
done

# Fichiers Java non référencés
find backend/src/main/java -name "*.java" -type f | while read f; do
  classname=$(basename "$f" .java)
  count=$(grep -rn "$classname" backend/src/main/java --include="*.java" | wc -l)
  if [ "$count" -le 1 ]; then
    echo "Potentiellement orphelin: $f"
  fi
done
```

### 6. Vérifier les assets non utilisés

```bash
# Images
find frontend/src -name "*.png" -o -name "*.jpg" -o -name "*.svg" | while read f; do
  basename=$(basename "$f")
  if ! grep -rq "$basename" frontend/src --include="*.ts" --include="*.html" --include="*.css"; then
    echo "Asset potentiellement non utilisé: $f"
  fi
done
```

### 7. Nettoyer les dépendances de développement en production

#### Backend - Vérifier les scopes Maven

```bash
grep -A5 "<dependency>" backend/pom.xml | grep -B5 "<scope>test</scope>"
```

#### Frontend - Vérifier devDependencies vs dependencies

```bash
cat frontend/package.json | jq '.dependencies, .devDependencies'
```

## Rapport de Code Mort

| Catégorie | Nombre | Fichiers/Classes | Action |
|-----------|--------|------------------|--------|
| Classes non utilisées | ? | | Supprimer |
| Méthodes non utilisées | ? | | Supprimer |
| Imports inutilisés | ? | | Nettoyer |
| Dépendances Maven inutilisées | ? | | Supprimer |
| Dépendances npm inutilisées | ? | | Supprimer |
| Assets non utilisés | ? | | Supprimer |

## Critères de Validation

- [ ] Aucune classe/composant orphelin
- [ ] Imports nettoyés
- [ ] Dépendances Maven analysées
- [ ] Dépendances npm analysées
- [ ] Assets vérifiés

## Étape Suivante

→ [Étape 9 : Vérification Qualité et Tests](./step-09-quality-tests.prompt.md)
