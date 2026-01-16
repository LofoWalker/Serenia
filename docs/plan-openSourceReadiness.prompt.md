# Plan : Vérification Open Source Readiness — Serenia

> **Objectif** : Évaluer et préparer le repository Serenia pour une publication en open source en vérifiant tous les aspects techniques, sécuritaires, légaux et organisationnels.

## 📂 Étapes Détaillées

Ce plan a été découpé en 11 étapes exécutables individuellement :

| # | Étape | Fichier |
|---|-------|---------|
| 1 | Scan des Secrets Exposés | [step-01-secrets-scan.prompt.md](./open-source-readiness/step-01-secrets-scan.prompt.md) |
| 2 | Anonymisation des Données Personnelles | [step-02-personal-data.prompt.md](./open-source-readiness/step-02-personal-data.prompt.md) |
| 3 | Scan et Nettoyage Historique Git | [step-03-git-history.prompt.md](./open-source-readiness/step-03-git-history.prompt.md) |
| 4 | Création des Fichiers Légaux | [step-04-legal-files.prompt.md](./open-source-readiness/step-04-legal-files.prompt.md) |
| 5 | Enrichissement du README | [step-05-readme.prompt.md](./open-source-readiness/step-05-readme.prompt.md) |
| 6 | Audit des Vulnérabilités (CVE) | [step-06-cve-audit.prompt.md](./open-source-readiness/step-06-cve-audit.prompt.md) |
| 7 | Vérification des Commentaires | [step-07-comments-review.prompt.md](./open-source-readiness/step-07-comments-review.prompt.md) |
| 8 | Analyse du Code Mort | [step-08-dead-code.prompt.md](./open-source-readiness/step-08-dead-code.prompt.md) |
| 9 | Vérification Qualité et Tests | [step-09-quality-tests.prompt.md](./open-source-readiness/step-09-quality-tests.prompt.md) |
| 10 | Uniformité et Standards | [step-10-code-standards.prompt.md](./open-source-readiness/step-10-code-standards.prompt.md) |
| 11 | Checklist Finale et Verdict | [step-11-final-checklist.prompt.md](./open-source-readiness/step-11-final-checklist.prompt.md) |

👉 **[Index complet des étapes](./open-source-readiness/README.md)**

---

## Vue d'Ensemble

Cette procédure couvre 7 axes majeurs de vérification :

| # | Axe | Priorité | Bloquant |
|---|-----|----------|----------|
| 1 | Sécurité | 🔴 Critique | Oui |
| 2 | Qualité du Code | 🟡 Haute | Partiel |
| 3 | Uniformité et Standards | 🟢 Moyenne | Non |
| 4 | Documentation | 🔴 Critique | Oui |
| 5 | Code Mort et Obsolète | 🟢 Moyenne | Non |
| 6 | Commentaires | 🟡 Haute | Partiel |
| 7 | Historique Git | 🔴 Critique | Oui |

---

## 1. Sécurité

### Objectif
S'assurer qu'aucune information sensible ne sera exposée publiquement.

### Points de Contrôle

#### 1.1 Secrets Exposés (API keys, tokens, credentials)
**Statut : 🔴 BLOQUANT**

| Point | Méthode de Vérification | Outil |
|-------|-------------------------|-------|
| Clés API dans le code | Recherche par pattern | `truffleHog`, `gitleaks` |
| Tokens d'authentification | Scan automatisé | `git-secrets` |
| Mots de passe hardcodés | Grep + Regex | `grep -rn "password\|secret\|api_key"` |
| Fichiers `.env` | Vérification manuelle | `.gitignore` check |

**⚠️ Problèmes Identifiés :**
- **Clés cryptographiques présentes** dans `backend/keys/` :
  - `privateKey.pem`
  - `publicKey.pem`
  - `rsaPrivateKey.pem`

**Actions Correctives :**
```bash
# Supprimer les clés du repository
rm -rf backend/keys/*.pem

# Ajouter au .gitignore
echo "backend/keys/*.pem" >> .gitignore

# Scanner l'historique
truffleHog git file://. --only-verified
gitleaks detect --source . --verbose
```

**Risque si non-conformité :** Compromission de l'authentification JWT, accès non autorisé aux données utilisateurs.

#### 1.2 Vulnérabilités Connues (CVE)
**Statut : 🟡 NON BLOQUANT (mais recommandé)**

| Composant | Outil | Commande |
|-----------|-------|----------|
| Backend (Java/Maven) | OWASP Dependency Check | `./mvnw org.owasp:dependency-check-maven:check` |
| Frontend (npm) | npm audit | `npm audit --audit-level=high` |
| Images Docker | Trivy | `trivy image serenia-backend:latest` |

**Actions Correctives :**
```bash
# Backend
cd backend && ./mvnw versions:display-dependency-updates

# Frontend
cd frontend && npm audit fix
```

#### 1.3 Données Sensibles ou Personnelles
**Statut : 🔴 BLOQUANT**

**⚠️ Problèmes Identifiés :**
- Email personnel exposé dans le code source :

| Fichier | Ligne | Valeur |
|---------|-------|--------|
| `frontend/src/app/features/privacy-policy/privacy-policy.component.ts` | 22, 37 | `tom1997walker@gmail.com` |
| `frontend/src/app/features/legal-notices/legal-notices.component.ts` | 23 | `tom1997walker@gmail.com` |
| `frontend/src/app/features/terms-of-service/terms-of-service.component.html` | 192 | `tom1997walker@gmail.com` |

**Actions Correctives :**
```bash
# Rechercher toutes les occurrences
grep -rn "tom1997walker@gmail.com" .

# Remplacer par un email générique
find . -type f \( -name "*.ts" -o -name "*.html" \) -exec sed -i 's/tom1997walker@gmail.com/contact@serenia.studio/g' {} +
```

#### 1.4 Surface d'Attaque et Configurations Dangereuses
**Statut : 🟡 NON BLOQUANT**

| Point | Vérification |
|-------|--------------|
| CORS trop permissifs | Vérifier `application.properties` |
| Debug mode activé | `quarkus.log.level` doit être `INFO` en prod |
| Endpoints exposés | Vérifier `/q/dev`, `/q/swagger` |

---

## 2. Qualité du Code

### Objectif
Garantir un code maintenable, lisible et testable pour la communauté open source.

### Points de Contrôle

#### 2.1 Lisibilité et Maintenabilité
**Statut : 🟢 NON BLOQUANT**

| Critère | Outil | Seuil Acceptable |
|---------|-------|------------------|
| Complexité cyclomatique | SonarQube | < 15 par méthode |
| Taille des classes | SonarQube | < 500 lignes |
| Profondeur d'héritage | Analyse manuelle | < 4 niveaux |

**Commande :**
```bash
# Backend
./mvnw sonar:sonar -Dsonar.projectKey=serenia

# Frontend
npx eslint --ext .ts,.html src/ --format json
```

#### 2.2 Tests (Présence, Couverture, Fiabilité)
**Statut : 🟡 PARTIELLEMENT BLOQUANT**

| Métrique | Seuil Minimum | Seuil Recommandé |
|----------|---------------|------------------|
| Couverture globale | 60% | 80% |
| Tests unitaires | Présents | ✅ |
| Tests d'intégration | Présents | ✅ |
| Tests E2E | Optionnel | 🟡 |

**Commandes :**
```bash
# Backend
cd backend && ./mvnw test jacoco:report
# Rapport : target/site/jacoco/index.html

# Frontend
cd frontend && npm run test -- --coverage
```

#### 2.3 Gestion des Erreurs
**Statut : 🟢 NON BLOQUANT**

- Vérifier la présence de handlers globaux d'exceptions
- S'assurer qu'aucune stack trace n'est exposée en production

**Fichiers à vérifier :**
- `backend/src/main/java/com/lofo/serenia/exception/`

---

## 3. Uniformité et Standards du Code

### Objectif
Assurer une cohérence de style permettant aux contributeurs de s'intégrer facilement.

### Points de Contrôle

#### 3.1 Conventions de Nommage
**Statut : 🟢 NON BLOQUANT**

| Langage | Convention | Vérification |
|---------|------------|--------------|
| Java | camelCase (variables), PascalCase (classes) | Checkstyle |
| TypeScript | camelCase | ESLint |
| CSS | kebab-case | Stylelint |

#### 3.2 Linting et Formatage
**Statut : 🟢 NON BLOQUANT**

**Configuration recommandée :**
```bash
# Backend - ajouter au pom.xml
# Plugin Checkstyle + Google Style Guide

# Frontend - vérifier la présence de
cat frontend/.eslintrc.json
cat frontend/.prettierrc
```

**Actions Correctives :**
```bash
# Frontend
cd frontend && npx prettier --write "src/**/*.{ts,html,css}"
npx eslint --fix "src/**/*.ts"
```

---

## 4. Documentation

### Objectif
Fournir une documentation complète permettant l'installation, l'utilisation et la contribution.

### Points de Contrôle

#### 4.1 Fichiers Standards Open Source
**Statut : 🔴 BLOQUANT**

| Fichier | Présent | Action |
|---------|---------|--------|
| `LICENSE` | ❌ Non | À créer (MIT/Apache 2.0 recommandé) |
| `CONTRIBUTING.md` | ❌ Non | À créer |
| `CODE_OF_CONDUCT.md` | ❌ Non | À créer (Contributor Covenant recommandé) |
| `SECURITY.md` | ❌ Non | À créer |
| `README.md` | ✅ Oui | À enrichir |

**Templates recommandés :**
- LICENSE : https://choosealicense.com/
- CODE_OF_CONDUCT : https://www.contributor-covenant.org/
- SECURITY : https://github.com/github/security-policy-template

#### 4.2 README
**Statut : 🔴 BLOQUANT**

Le README doit contenir :
- [ ] Description du projet
- [ ] Badges (CI, coverage, license)
- [ ] Prérequis
- [ ] Instructions d'installation
- [ ] Configuration
- [ ] Usage
- [ ] Comment contribuer
- [ ] Licence

---

## 5. Code Mort et Obsolète

### Objectif
Nettoyer le repository de tout code inutile qui alourdirait la maintenance.

### Points de Contrôle

#### 5.1 Fonctions Non Utilisées
**Statut : 🟢 NON BLOQUANT**

**Outils :**
```bash
# Backend - Détection de code mort
./mvnw spotbugs:check

# Frontend - Détection de code non utilisé
npx ts-prune
npx depcheck
```

#### 5.2 Dépendances Inutiles
**Statut : 🟢 NON BLOQUANT**

```bash
# Backend
./mvnw dependency:analyze

# Frontend
npx depcheck
```

---

## 6. Commentaires Oubliés ou Inappropriés

### Objectif
S'assurer qu'aucun commentaire ne révèle d'informations sensibles ou inappropriées.

### Points de Contrôle

#### 6.1 TODO/FIXME Non Traités
**Statut : 🟡 PARTIELLEMENT BLOQUANT**

```bash
# Rechercher les TODO/FIXME
grep -rn "TODO\|FIXME\|HACK\|XXX" --include="*.java" --include="*.ts" .
```

**Action :** Résoudre ou supprimer avant publication.

#### 6.2 Commentaires Sensibles
**Statut : 🔴 BLOQUANT**

Rechercher :
- Noms de personnes
- Références à des systèmes internes
- Commentaires non professionnels

```bash
grep -rn "@author\|internal\|private\|secret" --include="*.java" --include="*.ts" .
```

---

## 7. Historique Git

### Objectif
S'assurer que l'historique Git ne contient pas d'informations sensibles.

### Points de Contrôle

#### 7.1 Secrets dans l'Historique
**Statut : 🔴 BLOQUANT**

```bash
# Scanner tout l'historique
gitleaks detect --source . --verbose --log-opts="--all"
truffleHog git file://. --since-commit HEAD~1000
```

**Si secrets trouvés :**
```bash
# Option 1 : BFG Repo Cleaner (recommandé)
bfg --delete-files "*.pem" --no-blob-protection
bfg --replace-text passwords.txt

# Option 2 : git filter-branch
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch backend/keys/*.pem" \
  --prune-empty --tag-name-filter cat -- --all
```

#### 7.2 Messages de Commits
**Statut : 🟢 NON BLOQUANT**

Vérifier que les messages de commit ne contiennent pas :
- Informations personnelles
- Références à des tickets internes privés
- Langage inapproprié

```bash
git log --oneline | grep -i "internal\|private\|secret"
```

#### 7.3 Branches Obsolètes
**Statut : 🟢 NON BLOQUANT**

```bash
# Lister les branches fusionnées
git branch --merged main

# Supprimer les branches obsolètes
git branch -d <branch-name>
```

---

## Checklist Finale Récapitulative

### 🔴 Bloquants (à résoudre impérativement)

- [ ] Supprimer les clés PEM de `backend/keys/`
- [ ] Remplacer l'email personnel `tom1997walker@gmail.com` par un email générique
- [ ] Créer le fichier `LICENSE` (MIT ou Apache 2.0)
- [ ] Créer le fichier `CONTRIBUTING.md`
- [ ] Créer le fichier `CODE_OF_CONDUCT.md`
- [ ] Créer le fichier `SECURITY.md`
- [ ] Scanner et nettoyer l'historique Git des secrets
- [ ] Vérifier l'absence de commentaires sensibles

### 🟡 Recommandés (fortement conseillés)

- [ ] Atteindre 70%+ de couverture de tests
- [ ] Résoudre les TODO/FIXME critiques
- [ ] Exécuter `npm audit` et corriger les vulnérabilités hautes
- [ ] Exécuter OWASP Dependency Check sur le backend

### 🟢 Optionnels (améliorations)

- [ ] Configurer des badges dans le README (CI, coverage)
- [ ] Ajouter un fichier `.editorconfig`
- [ ] Supprimer le code mort détecté
- [ ] Nettoyer les branches Git obsolètes

---

## Verdict Final

| Critère | Statut Actuel |
|---------|---------------|
| Sécurité | ❌ Non conforme |
| Documentation | ❌ Non conforme |
| Qualité du code | ✅ Acceptable |
| Historique Git | ⚠️ À vérifier |

### 🔴 VERDICT : NON PRÊT POUR L'OPEN SOURCE

**Prêt sous conditions** après :
1. Suppression des clés cryptographiques
2. Anonymisation des données personnelles
3. Création des fichiers légaux (LICENSE, etc.)
4. Nettoyage de l'historique Git

---

## Outils Recommandés

| Catégorie | Outil | Lien |
|-----------|-------|------|
| Secrets | TruffleHog | https://github.com/trufflesecurity/trufflehog |
| Secrets | Gitleaks | https://github.com/gitleaks/gitleaks |
| Dépendances | OWASP Dependency Check | https://owasp.org/www-project-dependency-check/ |
| Docker | Trivy | https://github.com/aquasecurity/trivy |
| Git History | BFG Repo-Cleaner | https://rtyley.github.io/bfg-repo-cleaner/ |
| Qualité | SonarQube | https://www.sonarqube.org/ |

---

## Références

- [Open Source Guide](https://opensource.guide/)
- [GitHub Open Source Guide](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-readmes)
- [OWASP Secure Coding Practices](https://owasp.org/www-project-secure-coding-practices-quick-reference-guide/)
- [Contributor Covenant](https://www.contributor-covenant.org/)

---

*Document généré le 16 janvier 2026 — Serenia Open Source Readiness Audit*
