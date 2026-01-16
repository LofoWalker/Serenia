# Architecture Technique — Serenia

## 1. Vue d'Ensemble

Serenia est une application web full-stack suivant une **architecture trois-tiers** containerisée, avec séparation claire entre le frontend Angular, le backend Quarkus et la base de données PostgreSQL.

```
┌─────────────────────────────────────────────────────────────────────┐
│                            INTERNET                                  │
└─────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         TRAEFIK (Reverse Proxy)                      │
│  • TLS Termination (Let's Encrypt)                                  │
│  • Routing: serenia.domain → Frontend                               │
│            api.serenia.domain → Backend                             │
└─────────────────────────────────────────────────────────────────────┘
                    │                           │
                    ▼                           ▼
     ┌──────────────────────┐     ┌──────────────────────────────────┐
     │      FRONTEND        │     │           BACKEND                 │
     │   Angular 21 + SPA   │────▶│   Quarkus 3.29 + Java 21         │
     │   TailwindCSS        │     │   REST API                        │
     │   Nginx              │     │   JWT Authentication              │
     └──────────────────────┘     └──────────────────────────────────┘
                                              │
                    ┌─────────────────────────┼─────────────────────────┐
                    │                         │                         │
                    ▼                         ▼                         ▼
          ┌─────────────────┐      ┌─────────────────┐      ┌─────────────────┐
          │   PostgreSQL    │      │   OpenAI API    │      │   Stripe API    │
          │   (Persistance) │      │   (Chat IA)     │      │   (Paiements)   │
          └─────────────────┘      └─────────────────┘      └─────────────────┘
```

---

## 2. Stack Technologique

### 2.1 Frontend

| Composant | Technologie | Version |
|-----------|-------------|---------|
| Framework | Angular | 21.x |
| Styling | TailwindCSS | 4.x |
| Build | Angular CLI | 21.x |
| Server | Nginx | Alpine |
| Tests | Vitest | 4.x |

**Structure du Frontend :**
```
frontend/src/app/
├── core/                    # Services, guards, interceptors
│   ├── guards/              # Route guards (auth)
│   ├── interceptors/        # HTTP interceptors (JWT)
│   ├── models/              # Interfaces TypeScript
│   ├── services/            # Services injectables
│   └── validators/          # Validateurs custom
├── features/                # Modules fonctionnels
│   ├── auth/                # Login, register, password reset
│   ├── chat/                # Interface de conversation
│   ├── home/                # Page d'accueil
│   └── profile/             # Gestion du profil
└── shared/                  # Composants réutilisables
```

### 2.2 Backend

| Composant | Technologie | Version |
|-----------|-------------|---------|
| Runtime | Java | 21 (LTS) |
| Framework | Quarkus | 3.29.x |
| ORM | Hibernate Panache | - |
| Migrations | Liquibase | - |
| Sécurité | SmallRye JWT | - |
| Documentation | SmallRye OpenAPI | - |

**Structure du Backend :**
```
backend/src/main/java/com/lofo/serenia/
├── config/                  # Configuration (JWT, Stripe, OpenAI)
├── exception/               # Gestion centralisée des erreurs
├── mapper/                  # MapStruct mappers (DTO ↔ Entity)
├── persistence/
│   ├── entity/              # Entités JPA
│   │   ├── conversation/    # Conversation, Message, ChatMessage
│   │   ├── subscription/    # Plan, Subscription
│   │   └── user/            # User, Token
│   └── repository/          # Repositories Panache
├── rest/
│   ├── dto/                 # DTOs (in/out)
│   └── resource/            # Endpoints REST
├── service/
│   ├── chat/                # Orchestration chat, encryption
│   ├── mail/                # Service email
│   ├── subscription/        # Stripe, quotas
│   └── user/                # Auth, JWT, registration
└── validation/              # Validateurs custom
```

### 2.3 Base de Données

| Composant | Technologie | Version |
|-----------|-------------|---------|
| SGBD | PostgreSQL | 16 Alpine |
| Migrations | Liquibase | YAML |

---

## 3. Modèle de Données

```
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│     users       │       │  conversations  │       │    messages     │
├─────────────────┤       ├─────────────────┤       ├─────────────────┤
│ id (PK)         │──────<│ id (PK)         │──────<│ id (PK)         │
│ email           │       │ user_id (FK)    │       │ conversation_id │
│ password        │       │ created_at      │       │ user_id (FK)    │
│ first_name      │       │ last_activity_at│       │ role            │
│ last_name       │       └─────────────────┘       │ encrypted_content│
│ is_activated    │                                  │ timestamp       │
│ role            │                                  └─────────────────┘
└─────────────────┘
        │
        │       ┌─────────────────┐       ┌─────────────────┐
        │       │  subscriptions  │       │     plans       │
        │       ├─────────────────┤       ├─────────────────┤
        └──────<│ id (PK)         │──────<│ id (PK)         │
                │ user_id (FK)    │       │ name            │
                │ plan_id (FK)    │       │ monthly_token   │
                │ tokens_used     │       │ daily_message   │
                │ messages_sent   │       │ price_cents     │
                │ stripe_*        │       │ stripe_price_id │
                └─────────────────┘       └─────────────────┘
```

---

## 4. Sécurité

### 4.1 Authentification & Autorisation

```
┌────────────────┐      ┌────────────────┐      ┌────────────────┐
│   Client       │      │   Backend      │      │   JWT          │
│   (Angular)    │      │   (Quarkus)    │      │   Validation   │
└───────┬────────┘      └───────┬────────┘      └───────┬────────┘
        │                       │                       │
        │  POST /auth/login     │                       │
        │──────────────────────>│                       │
        │                       │                       │
        │                       │  Validate credentials │
        │                       │──────────────────────>│
        │                       │                       │
        │                       │  Generate JWT (RSA)   │
        │                       │<──────────────────────│
        │                       │                       │
        │  JWT Token            │                       │
        │<──────────────────────│                       │
        │                       │                       │
        │  GET /api/* + Bearer  │                       │
        │──────────────────────>│                       │
        │                       │  Verify JWT signature │
        │                       │──────────────────────>│
        │                       │                       │
```

- **Algorithme** : RS256 (RSA + SHA-256)
- **Clés** : RSA 2048 bits (stockées dans Docker Secrets)
- **Expiration** : Configurable via `SERENIA_AUTH_EXPIRATION_TIME`

### 4.2 Chiffrement des Messages

Le chiffrement des messages utilise **AES-256-GCM avec dérivation de clé par utilisateur via HKDF**.

#### Architecture de chiffrement

```
┌─────────────────────────────────────────────────────────────────────┐
│                         MASTER KEY                                   │
│              (SERENIA_SECURITY_KEY - AES-256)                       │
│              Stockée en secret Docker/env var                        │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      HKDF-SHA256                                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │ Input Key Material (IKM) : Master Key (32 bytes)            │    │
│  │ Salt                     : User ID (UUID as bytes)          │    │
│  │ Info                     : "serenia-user-encryption-v1"     │    │
│  │ Output Length            : 32 bytes (AES-256)               │    │
│  └─────────────────────────────────────────────────────────────┘    │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
           ┌───────────────────┼───────────────────┐
           ▼                   ▼                   ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   User Key A    │  │   User Key B    │  │   User Key C    │
│   (Derived)     │  │   (Derived)     │  │   (Derived)     │
└────────┬────────┘  └────────┬────────┘  └────────┬────────┘
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│   AES-256-GCM   │  │   AES-256-GCM   │  │   AES-256-GCM   │
│   Encrypt/      │  │   Encrypt/      │  │   Encrypt/      │
│   Decrypt       │  │   Decrypt       │  │   Decrypt       │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

#### Spécifications techniques

| Paramètre | Valeur |
|-----------|--------|
| **Algorithme** | AES-256-GCM |
| **IV** | 12 bytes (généré aléatoirement) |
| **Tag d'authentification** | 128 bits |
| **Dérivation de clé** | HKDF-SHA256 (RFC 5869) |
| **Contexte HKDF** | `serenia-user-encryption-v1` |

#### Format du payload chiffré (v1)

```
┌───────────────────────────────────────────────────────────────┐
│ Version (1 byte) │ IV (12 bytes) │ Ciphertext + Auth Tag     │
└───────────────────────────────────────────────────────────────┘

Version = 0x01 pour le schéma HKDF per-user
```

#### Rétrocompatibilité

Le système détecte automatiquement les anciens messages (format legacy sans version byte) et les déchiffre avec la clé maître directe. Les nouveaux messages sont toujours chiffrés avec le format v1 (HKDF per-user).

#### Bénéfices

- **Isolation cryptographique** : Chaque utilisateur possède une clé unique
- **Limitation d'impact** : Compromission d'une clé n'expose qu'un seul utilisateur
- **Aucune migration** : Clés dérivées déterministiquement, pas de stockage supplémentaire
- **Performance** : Dérivation HKDF négligeable (~1μs par opération)

---

## 5. Intégrations Externes

### 5.1 OpenAI API

```
┌─────────────────┐        ┌─────────────────┐        ┌─────────────────┐
│  ChatOrchestrator│        │ ChatCompletion  │        │   OpenAI API    │
│                 │        │    Service      │        │                 │
└───────┬─────────┘        └───────┬─────────┘        └───────┬─────────┘
        │                          │                          │
        │ generateReply(history)   │                          │
        │─────────────────────────>│                          │
        │                          │                          │
        │                          │  POST /chat/completions  │
        │                          │─────────────────────────>│
        │                          │                          │
        │                          │  Response + token usage  │
        │                          │<─────────────────────────│
        │                          │                          │
        │  ChatCompletionResult    │                          │
        │<─────────────────────────│                          │
```

### 5.2 Stripe

**Flux de souscription :**
1. Création Checkout Session
2. Redirection Stripe
3. Webhook `checkout.session.completed`
4. Mise à jour abonnement local

---

## 6. Déploiement

### 6.1 Architecture Docker

```yaml
services:
  traefik:     # Reverse proxy + TLS
  postgres:    # Base de données
  backend:     # API Quarkus
  frontend:    # SPA Angular + Nginx
```

### 6.2 Secrets Management

| Secret | Usage |
|--------|-------|
| `db_password` | Mot de passe PostgreSQL |
| `jwt_private_key.pem` | Signature JWT |
| `jwt_public_key.pem` | Vérification JWT |
| `security_key` | Chiffrement AES |
| `openai_api_key` | API OpenAI |
| `stripe_secret_key` | API Stripe |
| `stripe_webhook_secret` | Validation webhooks |
| `smtp_password` | Envoi emails |

### 6.3 Healthchecks

- **Backend** : `/q/health/live` et `/q/health/ready`
- **PostgreSQL** : `pg_isready`

---

## 7. API REST

### Endpoints Principaux

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| POST | `/auth/login` | Authentification | Non |
| POST | `/auth/register` | Inscription | Non |
| GET | `/conversation` | Liste conversations | Oui |
| POST | `/conversation/message` | Envoyer message | Oui |
| GET | `/subscription/status` | Statut abonnement | Oui |
| POST | `/subscription/checkout` | Créer checkout | Oui |
| GET | `/profile` | Profil utilisateur | Oui |

---

## 8. Monitoring & Logging

- **Logs** : Format structuré, niveau configurable
- **Health** : SmallRye Health (Quarkus)
- **Métriques** : Intégration possible avec Prometheus

---

*Architecture documentée par l'équipe Serenia — Janvier 2026*

