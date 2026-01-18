# Étape 9 : Vérification Qualité du Code et Couverture de Tests

> **Priorité** : 🟡 Haute | **Bloquant** : Partiel

## Objectif

S'assurer que le code est de qualité suffisante pour être maintenu par une communauté open source et que les tests sont fiables.

## Actions à Exécuter

### 1. Exécuter tous les tests - Backend

```bash
cd backend
./mvnw clean test
```

#### Générer le rapport de couverture JaCoCo

```bash
./mvnw jacoco:report
# Rapport : target/site/jacoco/index.html
```

#### Vérifier la couverture minimale

```bash
./mvnw jacoco:check -Djacoco.line.coverage=0.60
```

### 2. Exécuter tous les tests - Frontend

```bash
cd frontend
npm test -- --coverage --watch=false
```

#### Rapport de couverture

Le rapport sera dans `coverage/lcov-report/index.html`.

### 3. Analyse statique - Backend

#### Avec SonarQube (si disponible)

```bash
./mvnw sonar:sonar \
  -Dsonar.projectKey=serenia \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=YOUR_TOKEN
```

#### Avec SpotBugs

```bash
./mvnw spotbugs:check
./mvnw spotbugs:gui  # Pour voir les résultats graphiquement
```

#### Avec Checkstyle

```bash
./mvnw checkstyle:check
```

### 4. Analyse statique - Frontend

#### ESLint

```bash
cd frontend
npx eslint --ext .ts,.html src/ --format stylish
```

#### Avec résumé des erreurs

```bash
npx eslint --ext .ts,.html src/ --format compact | grep -c "Error"
npx eslint --ext .ts,.html src/ --format compact | grep -c "Warning"
```

### 5. Vérifier la complexité cyclomatique

#### Backend - Avec PMD

```bash
./mvnw pmd:check
```

#### Frontend - Avec ESLint complexity rule

```bash
npx eslint --ext .ts src/ --rule 'complexity: ["error", 15]'
```

### 6. Vérifier les tests d'intégration

```bash
cd backend
./mvnw verify -Pintegration-tests
```

### 7. Vérifier la gestion des erreurs

```bash
# Rechercher les catch vides
grep -rn "catch.*{[[:space:]]*}" --include="*.java" backend/src/
grep -rn "catch.*{[[:space:]]*}" --include="*.ts" frontend/src/

# Rechercher les exceptions génériques
grep -rn "catch (Exception\|catch (Error\|catch (Throwable" --include="*.java" backend/src/
```

## Métriques de Qualité

| Métrique | Backend | Frontend | Seuil Minimum | Seuil Recommandé |
|----------|---------|----------|---------------|------------------|
| Couverture de tests | ? % | ? % | 60% | 80% |
| Tests passants | ?/? | ?/? | 100% | 100% |
| Erreurs ESLint/Checkstyle | ? | ? | 0 critiques | 0 |
| Bugs SpotBugs | ? | - | 0 critiques | 0 |
| Complexité max | ? | ? | ≤ 15 | ≤ 10 |

## Rapport de Qualité

### Tests

| Suite | Total | Passés | Échoués | Ignorés |
|-------|-------|--------|---------|---------|
| Backend Unit | | | | |
| Backend Integration | | | | |
| Frontend Unit | | | | |

### Couverture

| Module | Lignes | Branches | Méthodes |
|--------|--------|----------|----------|
| Backend | % | % | % |
| Frontend | % | % | % |

## Critères de Validation

- [ ] Tous les tests passent (Backend)
- [ ] Tous les tests passent (Frontend)
- [ ] Couverture Backend ≥ 60%
- [ ] Couverture Frontend ≥ 60%
- [ ] Aucune erreur critique ESLint
- [ ] Aucun bug critique SpotBugs
- [ ] Complexité cyclomatique ≤ 15

## Étape Suivante

→ [Étape 10 : Uniformité et Standards](./step-10-code-standards.prompt.md)
