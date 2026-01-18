# Step 1 : Analyse des Vulnérabilités Critiques et Élevées

## Vue d'Ensemble

Cette section couvre les **vulnérabilités critiques et élevées** identifiées lors de l'audit de sécurité de l'application Serenia. Elle est décomposée en **4 sous-étapes** indépendantes pour faciliter l'implémentation et le suivi.

---

## Sous-Étapes

| Step | Titre | Priorité | Effort Estimé | Statut |
|------|-------|----------|---------------|--------|
| [1.1](step_1_1_rate_limiting.md) | Implémentation du Rate Limiting | 🔴 CRITIQUE | 1-2 jours | ⬜ TODO |
| [1.2](step_1_2_jwt_httponly.md) | Migration JWT vers Cookie HttpOnly | 🟠 ÉLEVÉE | 3-5 jours | ⬜ TODO |
| [1.3](step_1_3_csp_hardening.md) | Renforcement de la CSP | 🟠 ÉLEVÉE | 2-3 jours | ⬜ TODO |
| [1.4](step_1_4_security_headers.md) | Headers de Sécurité HTTP (Backend) | 🟡 MOYENNE | 0.5-1 jour | ⬜ TODO |

---

## Contexte Global

L'application Serenia est une plateforme de chat IA composée d'un backend Java 21/Quarkus 3.29, d'un frontend Angular 21, et d'une base PostgreSQL 16. L'architecture actuelle présente des fondamentaux de sécurité solides (chiffrement AES-256-GCM, bcrypt, JWT RSA), mais plusieurs vulnérabilités de niveau moyen à élevé ont été identifiées nécessitant une attention immédiate.

### Résumé des Vulnérabilités

| ID | Vulnérabilité | Fichier(s) Concerné(s) | Sous-Step |
|----|---------------|------------------------|-----------|
| V-1.1 | Rate limiting absent (brute-force) | `AuthenticationResource.java`, `ConversationResource.java` | [1.1](step_1_1_rate_limiting.md) |
| V-1.2 | Token JWT en sessionStorage (XSS) | `auth-state.service.ts` | [1.2](step_1_2_jwt_httponly.md) |
| V-1.3 | CSP avec `'unsafe-inline'` | `nginx.conf` | [1.3](step_1_3_csp_hardening.md) |
| V-1.4 | Headers de sécurité absents (API) | `compose.yaml`, `middlewares.yml` | [1.4](step_1_4_security_headers.md) |

---

## Objectifs Consolidés

1. **Implémenter un rate limiting** au niveau applicatif et/ou infrastructure pour protéger contre les attaques DoS et brute-force → **Step 1.1**
2. **Migrer le stockage du token JWT** vers un cookie `httpOnly` sécurisé ou évaluer l'acceptation du risque actuel → **Step 1.2**
3. **Renforcer la CSP** en supprimant `'unsafe-inline'` et en utilisant des hashes/nonces → **Step 1.3**
4. **Appliquer des headers de sécurité** au backend via Traefik ou directement dans Quarkus → **Step 1.4**

---

## Ordre de Réalisation Recommandé

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    ORDRE D'IMPLÉMENTATION                                │
└─────────────────────────────────────────────────────────────────────────┘

     Semaine 1                    Semaine 2                    Semaine 3
  ┌─────────────┐             ┌─────────────┐             ┌─────────────┐
  │  Step 1.1   │             │  Step 1.2   │             │  Step 1.3   │
  │   Rate      │────────────►│    JWT      │────────────►│    CSP      │
  │  Limiting   │             │  HttpOnly   │             │ Hardening   │
  │  (1-2j)     │             │  (3-5j)     │             │  (2-3j)     │
  └─────────────┘             └─────────────┘             └─────────────┘
        │                                                       │
        │                   ┌─────────────┐                    │
        │                   │  Step 1.4   │                    │
        └──────────────────►│  Security   │◄───────────────────┘
                            │  Headers    │
                            │  (0.5-1j)   │
                            └─────────────┘
                                   │
                                   ▼
                            Peut être fait en
                            parallèle avec 1.1
```

**Justification de l'ordre :**

1. **Step 1.1 (Rate Limiting)** - PREMIER : Bloque les attaques brute-force immédiatement, sans dépendance
2. **Step 1.4 (Headers)** - Peut être fait en parallèle avec 1.1 (configuration Traefik)
3. **Step 1.2 (JWT HttpOnly)** - Nécessite des modifications backend ET frontend, plus complexe
4. **Step 1.3 (CSP)** - Peut casser l'application si mal configuré, à faire après tests complets

---

## Résumé des Méthodes (Détails dans chaque sous-step)

### 1.1 Rate Limiting
- **Traefik** : Middlewares `rate-limit-auth` (5 req/s) et `rate-limit-api` (30 req/s)
- **Applicatif** (optionnel) : Bucket4j avec filtre JAX-RS
- 📄 Voir [step_1_1_rate_limiting.md](step_1_1_rate_limiting.md)

### 1.2 JWT HttpOnly Cookie
- **Backend** : Retourner le token via `Set-Cookie: HttpOnly; Secure; SameSite=Strict`
- **Frontend** : Supprimer le stockage sessionStorage, utiliser `withCredentials: true`
- 📄 Voir [step_1_2_jwt_httponly.md](step_1_2_jwt_httponly.md)

### 1.3 CSP Hardening
- Supprimer `'unsafe-inline'` de script-src et style-src
- Utiliser des hashes SHA-256 pour les scripts/styles légitimes si nécessaire
- 📄 Voir [step_1_3_csp_hardening.md](step_1_3_csp_hardening.md)

### 1.4 Security Headers Backend
- Ajouter middleware Traefik `security-headers-api`
- Headers : HSTS, X-Frame-Options, X-Content-Type-Options, CSP, etc.
- 📄 Voir [step_1_4_security_headers.md](step_1_4_security_headers.md)

---

## Architecture Globale

```
┌──────────────────────────────────────────────────────────────────┐
│                         INTERNET                                  │
└──────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│                     TRAEFIK (Reverse Proxy)                       │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Middlewares:                                                │ │
│  │  - rate-limit-auth (5 req/s pour /auth/*) → Step 1.1        │ │
│  │  - rate-limit-api (30 req/s pour /*)      → Step 1.1        │ │
│  │  - security-headers-api (HSTS, CSP, etc.) → Step 1.4        │ │
│  └─────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
                     │                          │
                     ▼                          ▼
┌────────────────────────────┐    ┌────────────────────────────────┐
│       FRONTEND (Nginx)     │    │        BACKEND (Quarkus)       │
│  ┌──────────────────────┐  │    │  ┌──────────────────────────┐  │
│  │  CSP sans unsafe-*   │  │    │  │  JWT HttpOnly Cookie     │  │
│  │  → Step 1.3          │  │    │  │  → Step 1.2              │  │
│  └──────────────────────┘  │    │  └──────────────────────────┘  │
└────────────────────────────┘    └────────────────────────────────┘
```

Voir les diagrammes détaillés dans chaque sous-step.

---

## Tests d'Acceptance (Résumé)

Les tests détaillés avec scripts sont dans chaque sous-step. Voici un récapitulatif :

### TA-1.1 : Rate Limiting
| Test | Sous-Step | Critère |
|------|-----------|---------|
| Brute-force auth bloqué | [1.1](step_1_1_rate_limiting.md#tests-dacceptance) | 429 après 10 req/s |
| Flood API bloqué | [1.1](step_1_1_rate_limiting.md#tests-dacceptance) | 429 après 50 req/s |

### TA-1.2 : JWT HttpOnly
| Test | Sous-Step | Critère |
|------|-----------|---------|
| Cookie HttpOnly | [1.2](step_1_2_jwt_httponly.md#tests-dacceptance) | `document.cookie` vide |
| Login/Logout fonctionnel | [1.2](step_1_2_jwt_httponly.md#tests-dacceptance) | Flux complet OK |

### TA-1.3 : CSP
| Test | Sous-Step | Critère |
|------|-----------|---------|
| Script inline bloqué | [1.3](step_1_3_csp_hardening.md#tests-dacceptance) | Erreur CSP console |
| App fonctionne | [1.3](step_1_3_csp_hardening.md#tests-dacceptance) | 0 erreurs CSP |

### TA-1.4 : Headers
| Test | Sous-Step | Critère |
|------|-----------|---------|
| Headers présents | [1.4](step_1_4_security_headers.md#tests-dacceptance) | X-Frame-Options, HSTS, etc. |
| Mozilla Observatory | [1.4](step_1_4_security_headers.md#tests-dacceptance) | Score >= A |

---

## Critères de Complétion

- [ ] Rate limiting actif sur `/auth/*` (5 req/s) et `/conversations/*` (30 req/s)
- [ ] Token JWT stocké en cookie HttpOnly (ou risque formellement accepté avec documentation)
- [ ] CSP sans `'unsafe-inline'` - utilisation de hashes SHA-256
- [ ] Headers de sécurité appliqués au backend via Traefik
- [ ] Tests d'acceptance TA-1.1 à TA-1.4 passent à 100%
- [ ] Score Mozilla Observatory >= A
