# Step 6 : Scan des Dépendances et Rapport Final

## Contexte

La sécurité d'une application ne repose pas uniquement sur le code applicatif mais également sur l'ensemble de sa chaîne de dépendances. Les vulnérabilités connues (CVE) dans les bibliothèques tierces représentent un vecteur d'attaque majeur.

### Stack technologique à auditer

| Composant | Technologie | Version | Gestionnaire |
|-----------|-------------|---------|--------------|
| Backend | Java 21 / Quarkus | 3.29.2 | Maven |
| Frontend | Angular | 21 | npm |
| Database | PostgreSQL | 16-alpine | Docker |
| Reverse Proxy | Traefik | 3.6.1 | Docker |
| Payments | Stripe SDK | 28.2.0 | Maven |

---

## Objectif

1. **Scanner les dépendances backend** : Identifier les CVE dans les dépendances Maven
2. **Scanner les dépendances frontend** : Identifier les CVE dans les packages npm
3. **Vérifier les images Docker** : Vulnérabilités dans les images de base
4. **Consolider les findings** : Produire un rapport de sécurité complet
5. **Établir un plan de remédiation** : Prioriser les correctifs

---

## Méthode

### 6.1 Scan des Dépendances Maven (Backend)

#### Configuration OWASP Dependency-Check

Ajouter dans `pom.xml` si non présent :

```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.9</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
        <formats>
            <format>HTML</format>
            <format>JSON</format>
        </formats>
        <suppressionFiles>
            <suppressionFile>dependency-check-suppression.xml</suppressionFile>
        </suppressionFiles>
    </configuration>
</plugin>
```

#### Exécution du scan

```bash
cd backend

# Scan complet avec rapport
./mvnw dependency-check:check

# Rapport généré dans target/dependency-check-report.html
```

#### Dépendances critiques à surveiller

| Dépendance | Version Actuelle | Risque | CVE Connues |
|------------|-----------------|--------|-------------|
| `quarkus-bom` | 3.29.2 | Critique | Vérifier |
| `smallrye-jwt` | (BOM) | Élevé | Auth bypass potentiel |
| `hibernate-orm` | (BOM) | Élevé | SQL injection |
| `stripe-java` | 28.2.0 | Élevé | Payment security |
| `openai-java` | x.x.x | Moyen | API security |
| `bcprov-jdk18on` | x.x.x | Élevé | Crypto vulnerabilities |

### 6.2 Scan des Dépendances npm (Frontend)

#### Exécution de npm audit

```bash
cd frontend

# Audit de base
npm audit

# Audit avec format JSON pour parsing
npm audit --json > npm-audit-report.json

# Correction automatique des vulnérabilités mineures
npm audit fix

# Voir les vulnérabilités qui nécessitent des breaking changes
npm audit fix --dry-run --force
```

#### Packages critiques à surveiller

| Package | Version Actuelle | Risque | Vulnérabilités Typiques |
|---------|-----------------|--------|------------------------|
| `@angular/core` | 21.x | Critique | XSS, Template injection |
| `rxjs` | x.x.x | Moyen | Prototype pollution |
| `zone.js` | x.x.x | Moyen | Various |
| Dépendances de build | * | Variable | Supply chain |

### 6.3 Scan des Images Docker

#### Utilisation de Trivy

```bash
# Installer Trivy si nécessaire
# brew install trivy (macOS)
# apt install trivy (Debian/Ubuntu)

# Scanner l'image backend
trivy image ${BACK_IMAGE}

# Scanner l'image frontend
trivy image ${FRONT_IMAGE}

# Scanner l'image PostgreSQL
trivy image postgres:16-alpine

# Scanner l'image Traefik
trivy image traefik:v3.6.1

# Rapport JSON
trivy image --format json --output trivy-report.json ${BACK_IMAGE}
```

#### Niveaux de sévérité Trivy

| Sévérité | Action |
|----------|--------|
| CRITICAL | Correction immédiate requise |
| HIGH | Correction dans 7 jours |
| MEDIUM | Correction dans 30 jours |
| LOW | Évaluation lors du prochain sprint |
| UNKNOWN | Investigation nécessaire |

### 6.4 Vérification des Versions

#### Versions à jour recommandées (Janvier 2026)

| Composant | Version Actuelle | Dernière Stable | Action |
|-----------|-----------------|-----------------|--------|
| Quarkus | 3.29.2 | Vérifier quarkus.io | ⬜ |
| Angular | 21 | Vérifier angular.io | ⬜ |
| PostgreSQL | 16 | 16.x ou 17.x | ⬜ |
| Traefik | 3.6.1 | Vérifier traefik.io | ⬜ |
| Java | 21 | 21 LTS (OK) | ✅ |
| Node.js | ? | 20 LTS ou 22 LTS | ⬜ |

#### Script de vérification des versions

```bash
#!/bin/bash
echo "=== Version Check ==="

# Java
java -version 2>&1 | head -1

# Node.js
node --version

# npm
npm --version

# Angular CLI
npx ng version 2>/dev/null | grep "Angular CLI"

# Quarkus
grep "<quarkus.platform.version>" backend/pom.xml

# PostgreSQL
docker exec serenia-db psql --version

# Traefik
docker exec serenia-traefik traefik version 2>/dev/null || echo "Check Traefik version"
```

### 6.5 Création du Fichier de Suppression (False Positives)

Si des CVE sont des faux positifs ou acceptés, créer `dependency-check-suppression.xml` :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
    <!-- Exemple: CVE non applicable car fonctionnalité non utilisée -->
    <suppress>
        <notes><![CDATA[
            This CVE affects feature X which is not used in this application.
            Risk accepted by: Security Team
            Date: 2026-01-18
        ]]></notes>
        <cve>CVE-YYYY-XXXXX</cve>
    </suppress>
</suppressions>
```

---

## Architecture

### Pipeline de sécurité recommandé

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    SECURITY SCANNING PIPELINE                            │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   COMMIT    │────►│    BUILD    │────►│    TEST     │────►│   DEPLOY    │
└─────────────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
                           │                   │                   │
                           ▼                   ▼                   ▼
                    ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
                    │   SAST      │     │   DAST      │     │  RUNTIME    │
                    │  Scanning   │     │  Scanning   │     │  Monitoring │
                    └─────────────┘     └─────────────┘     └─────────────┘
                           │                   │                   │
          ┌────────────────┼───────────────────┼───────────────────┤
          │                │                   │                   │
          ▼                ▼                   ▼                   ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
│  Dependency  │  │    Code      │  │    API       │  │   WAF/IDS        │
│    Check     │  │   Analysis   │  │   Testing    │  │   Alerts         │
│  (OWASP DC)  │  │  (SonarQube) │  │   (OWASP    │  │                  │
│              │  │              │  │    ZAP)      │  │                  │
└──────────────┘  └──────────────┘  └──────────────┘  └──────────────────┘
        │                │                 │                   │
        └────────────────┴─────────────────┴───────────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │   SECURITY DASHBOARD        │
                    │   - CVE tracking            │
                    │   - Remediation status      │
                    │   - Compliance metrics      │
                    └─────────────────────────────┘
```

### Intégration CI/CD (GitHub Actions exemple)

```yaml
# .github/workflows/security-scan.yml
name: Security Scan

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
  schedule:
    - cron: '0 6 * * 1'  # Weekly on Monday 6 AM

jobs:
  dependency-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: OWASP Dependency Check
        run: |
          cd backend
          ./mvnw dependency-check:check
      
      - name: Upload Dependency Check Report
        uses: actions/upload-artifact@v4
        with:
          name: dependency-check-report
          path: backend/target/dependency-check-report.html

  npm-audit:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
      
      - name: npm audit
        run: |
          cd frontend
          npm ci
          npm audit --audit-level=high
        continue-on-error: true  # Don't fail build, report only
      
      - name: Save audit report
        run: |
          cd frontend
          npm audit --json > npm-audit.json || true
      
      - name: Upload npm audit report
        uses: actions/upload-artifact@v4
        with:
          name: npm-audit-report
          path: frontend/npm-audit.json

  docker-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: 'postgres:16-alpine'
          format: 'table'
          exit-code: '0'
          ignore-unfixed: true
          vuln-type: 'os,library'
          severity: 'CRITICAL,HIGH'
```

---

## Tests d'Acceptance

### TA-6.1 : Scan Maven Réussi

| # | Scénario | Commande | Résultat Attendu |
|---|----------|----------|------------------|
| 1 | Scan exécuté | `./mvnw dependency-check:check` | Build SUCCESS |
| 2 | Rapport généré | `ls target/dependency-check-report.html` | Fichier présent |
| 3 | Pas de CRITICAL non-supprimées | Analyser rapport | 0 vulnérabilités CRITICAL |
| 4 | HIGH documentées | Analyser rapport | Toutes HIGH dans plan de remédiation |

### TA-6.2 : Scan npm Réussi

| # | Scénario | Commande | Résultat Attendu |
|---|----------|----------|------------------|
| 1 | Audit exécuté | `npm audit` | Rapport généré |
| 2 | Pas de critical | `npm audit --audit-level=critical` | Exit code 0 |
| 3 | High documentées | Analyser rapport | Dans plan de remédiation |

### TA-6.3 : Scan Docker Réussi

| # | Image | Commande | Résultat Attendu |
|---|-------|----------|------------------|
| 1 | Backend | `trivy image $BACK_IMAGE` | 0 CRITICAL |
| 2 | Frontend | `trivy image $FRONT_IMAGE` | 0 CRITICAL |
| 3 | PostgreSQL | `trivy image postgres:16-alpine` | 0 CRITICAL |
| 4 | Traefik | `trivy image traefik:v3.6.1` | 0 CRITICAL |

### TA-6.4 : Versions à Jour

| # | Composant | Vérification | Résultat Attendu |
|---|-----------|--------------|------------------|
| 1 | Quarkus | Comparer avec quarkus.io | ≤ 1 version mineure de retard |
| 2 | Angular | Comparer avec angular.io | Version LTS ou current |
| 3 | PostgreSQL | Comparer avec postgresql.org | Version majeure supportée |
| 4 | Traefik | Comparer avec traefik.io | ≤ 2 versions mineures de retard |

---

## Rapport Final de l'Audit

### Template de Rapport

```markdown
# Rapport d'Audit de Sécurité - Application Serenia
Date: YYYY-MM-DD
Auditeur: [Nom]
Version de l'application: X.Y.Z

## Résumé Exécutif

| Catégorie | Critique | Élevé | Moyen | Faible | Info |
|-----------|----------|-------|-------|--------|------|
| Vulnérabilités Code | 0 | X | X | X | X |
| CVE Dépendances | 0 | X | X | X | X |
| Configuration | 0 | X | X | X | X |
| **Total** | **0** | **X** | **X** | **X** | **X** |

## Score de Sécurité Global

🟢 **A** / 🟡 **B** / 🟠 **C** / 🔴 **D** / ⚫ **F**

## Points Forts Identifiés

1. ✅ Chiffrement AES-256-GCM avec HKDF pour isolation des données
2. ✅ Authentification JWT RS256 avec clés RSA
3. ✅ Docker Secrets pour la gestion des secrets
4. ✅ Validation signature Stripe sur webhooks
5. ✅ Contrôles d'accès IDOR via userId dans toutes les requêtes

## Vulnérabilités et Recommandations

### Critiques (Action Immédiate)
_Aucune vulnérabilité critique identifiée_

### Élevées (Correction sous 7 jours)

#### V-1: Rate Limiting Absent
- **Localisation**: `AuthenticationResource.java`, `ConversationResource.java`
- **Impact**: DoS, brute-force
- **Recommandation**: Implémenter rate limiting via Traefik et/ou applicatif
- **Effort**: 2 jours

#### V-2: Token JWT en sessionStorage
- **Localisation**: `auth-state.service.ts`
- **Impact**: Vol de token via XSS
- **Recommandation**: Migrer vers cookie HttpOnly ou accepter le risque
- **Effort**: 5 jours (migration) / 0 (acceptation documentée)

### Moyennes (Correction sous 30 jours)

#### V-3: CSP avec 'unsafe-inline'
- **Localisation**: `nginx.conf`
- **Impact**: Protection XSS affaiblie
- **Recommandation**: Utiliser hashes SHA-256 pour scripts/styles
- **Effort**: 3 jours

#### V-4: XSS Indirect via OpenAI
- **Localisation**: `ChatCompletionService.java`
- **Impact**: XSS si réponse IA contient du HTML
- **Recommandation**: Sanitizer les réponses OpenAI
- **Effort**: 1 jour

### Faibles (Planification)

#### V-5: Pas de table d'audit
- **Impact**: Traçabilité RGPD réduite
- **Recommandation**: Créer table `audit_logs`
- **Effort**: 2 jours

## Conformité

| Référentiel | Statut | Notes |
|-------------|--------|-------|
| OWASP Top 10 2021 | 🟡 Partiel | Injection ✅, Broken Auth ✅, XSS 🟡 |
| RGPD | 🟡 Partiel | Droit à l'effacement ✅, Portabilité ❌ |
| PCI-DSS (si applicable) | ✅ Délégué | Paiements gérés par Stripe |

## Plan de Remédiation

| # | Vulnérabilité | Priorité | Responsable | Deadline | Statut |
|---|---------------|----------|-------------|----------|--------|
| 1 | Rate limiting | P1 | DevOps | J+7 | ⬜ TODO |
| 2 | CSP hardening | P2 | Frontend | J+14 | ⬜ TODO |
| 3 | OpenAI sanitization | P2 | Backend | J+14 | ⬜ TODO |
| 4 | JWT HttpOnly | P2 | Full-stack | J+30 | ⬜ TODO |
| 5 | Audit table | P3 | Backend | J+60 | ⬜ TODO |

## Annexes

### A. Rapport OWASP Dependency Check
[Lien vers rapport HTML]

### B. Rapport npm audit
[Lien vers rapport JSON]

### C. Rapport Trivy
[Lien vers rapport]

### D. Méthodologie d'Audit
- OWASP Testing Guide v4.2
- OWASP ASVS v4.0
- CIS Benchmarks
```

---

## Critères de Complétion

- [ ] Scan OWASP Dependency Check exécuté et rapport généré
- [ ] npm audit exécuté et rapport généré
- [ ] Trivy scan sur toutes les images Docker
- [ ] Toutes les CVE CRITICAL corrigées ou documentées comme faux positifs
- [ ] Toutes les CVE HIGH dans le plan de remédiation avec deadline
- [ ] Rapport final consolidé produit
- [ ] Plan de remédiation avec responsables et deadlines
- [ ] Pipeline CI/CD de scan configuré (optionnel mais recommandé)
- [ ] Fichier de suppression des faux positifs créé si nécessaire
- [ ] Versions de toutes les dépendances documentées

---

## Checklist de Livraison de l'Audit

### Documents à produire

- [ ] `audit/step_1.md` - Vulnérabilités critiques et élevées
- [ ] `audit/step_2.md` - Contrôles d'accès et IDOR
- [ ] `audit/step_3.md` - Cryptographie et secrets
- [ ] `audit/step_4.md` - Intégrations tierces
- [ ] `audit/step_5.md` - Conformité RGPD
- [ ] `audit/step_6.md` - Dépendances et rapport final
- [ ] `audit/SECURITY_REPORT.md` - Rapport exécutif consolidé
- [ ] `dependency-check-suppression.xml` - Faux positifs documentés

### Artefacts techniques

- [ ] `target/dependency-check-report.html`
- [ ] `frontend/npm-audit.json`
- [ ] `trivy-reports/*.json`
- [ ] `.github/workflows/security-scan.yml` (si CI/CD configuré)

### Réunion de clôture

- [ ] Présentation des findings à l'équipe
- [ ] Validation du plan de remédiation
- [ ] Attribution des responsabilités
- [ ] Définition du calendrier de correction
- [ ] Planification de l'audit de suivi (recommandé : 3 mois)
