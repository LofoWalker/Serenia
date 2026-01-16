<p align="center">
  <img src="frontend/public/web-app-manifest-512x512.png" alt="Serenia Logo" width="120" height="120">
</p>

<h1 align="center">Serenia</h1>

<p align="center">
  <strong>Un lieu sûr pour te confier, discuter, et respirer.</strong>
</p>

<p align="center">
  <a href="#fonctionnalités">Fonctionnalités</a> •
  <a href="#démarrage-rapide">Démarrage</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#déploiement">Déploiement</a> •
  <a href="#documentation">Documentation</a>
</p>

---

## À Propos

**Serenia** est une application de chat conversationnel basée sur l'IA, offrant une expérience unique d'échange avec une intelligence artificielle au caractère authentique et décontracté.

Contrairement aux assistants IA traditionnels, Serenia adopte la personnalité d'un ami proche : naturel, parfois sarcastique, jamais professionnel. L'IA répond comme par SMS, en messages courts (max 180 caractères), avec un ton détendu.

## Fonctionnalités

### 💬 Chat Intelligent
- Conversations naturelles avec une IA au caractère unique
- Historique persistant et chiffré de bout en bout
- Contexte conversationnel maintenu

### 🔐 Sécurité Renforcée
- Chiffrement AES-256-GCM avec clé dérivée par utilisateur (HKDF)
- Authentification JWT (RSA)
- Transport HTTPS obligatoire

### 💳 Abonnements Flexibles
- Plan gratuit avec quotas journaliers
- Plans premium (Plus, Max) pour plus de messages
- Intégration Stripe complète

### 📧 Gestion de Compte
- Inscription avec vérification email
- Réinitialisation de mot de passe
- Profil utilisateur personnalisable

## Stack Technologique

| Composant | Technologies |
|-----------|--------------|
| **Backend** | Java 21, Quarkus 3.29, Hibernate Panache, Liquibase |
| **Frontend** | Angular 21, TailwindCSS 4, TypeScript |
| **Base de données** | PostgreSQL 16 |
| **Infrastructure** | Docker, Traefik, Nginx |
| **Services** | OpenAI API, Stripe |

## Prérequis

- **Docker** & **Docker Compose** (production)
- **Java 21** (développement backend)
- **Node.js 20+** (développement frontend)
- **PostgreSQL 16** (développement local)

## Démarrage Rapide

### Développement Local

#### Backend

```bash
cd backend

# Configurer les variables d'environnement
cp .env.example .env
# Éditer .env avec vos valeurs

# Démarrer en mode dev (hot reload)
./mvnw quarkus:dev
```

L'API sera disponible sur `http://localhost:8080`

#### Frontend

```bash
cd frontend

# Installer les dépendances
npm install

# Démarrer le serveur de développement
npm start
```

L'application sera disponible sur `http://localhost:4200`

### Production (Docker)

```bash
# Créer le dossier secrets et configurer les fichiers
mkdir -p secrets
echo "votre_mot_de_passe_db" > secrets/db_password
# ... configurer les autres secrets

# Configurer l'environnement
cp .env.example .env
# Éditer .env avec vos valeurs de production

# Démarrer tous les services
docker compose up -d
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        TRAEFIK                               │
│              (Reverse Proxy + TLS Let's Encrypt)            │
└──────────────────────┬──────────────────────────────────────┘
                       │
         ┌─────────────┴─────────────┐
         ▼                           ▼
┌─────────────────┐       ┌─────────────────────────┐
│    FRONTEND     │       │        BACKEND          │
│  Angular + Nginx│       │   Quarkus + Java 21     │
└─────────────────┘       └───────────┬─────────────┘
                                      │
              ┌───────────────────────┼───────────────────────┐
              ▼                       ▼                       ▼
     ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
     │   PostgreSQL    │    │   OpenAI API    │    │   Stripe API    │
     └─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Structure du Projet

```
Serenia/
├── compose.yaml          # Docker Compose (production)
├── docs/                 # Documentation
│   ├── prd.md           # Product Requirements Document
│   ├── architecture.md  # Architecture technique
│   └── global-info.md   # Informations globales
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/   # Code source Java
│       └── test/        # Tests
├── frontend/
│   ├── Dockerfile
│   ├── package.json
│   └── src/             # Code source Angular
└── traefik/             # Configuration reverse proxy
```

## Configuration

### Variables d'Environnement Principales

| Variable | Description |
|----------|-------------|
| `QUARKUS_DATASOURCE_JDBC_URL` | URL de connexion PostgreSQL |
| `OPENAI_API_KEY` | Clé API OpenAI |
| `OPENAI_MODEL` | Modèle OpenAI à utiliser |
| `STRIPE_SECRET_KEY` | Clé secrète Stripe |
| `SERENIA_SECURITY_KEY` | Clé de chiffrement AES |

Voir [docs/global-info.md](docs/global-info.md) pour la liste complète.

## API

L'API REST est documentée via OpenAPI. En mode développement, accédez à :

- **Swagger UI** : `http://localhost:8080/q/swagger-ui`
- **OpenAPI Spec** : `http://localhost:8080/q/openapi`

### Endpoints Principaux

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `POST` | `/auth/login` | Authentification |
| `POST` | `/auth/register` | Inscription |
| `GET` | `/conversation` | Liste des conversations |
| `POST` | `/conversation/message` | Envoyer un message |
| `GET` | `/subscription/status` | Statut abonnement |

## Tests

### Backend

```bash
cd backend

# Exécuter tous les tests
./mvnw test

# Tests avec couverture
./mvnw test jacoco:report
```

### Frontend

```bash
cd frontend

# Exécuter les tests
npm test
```

## Déploiement

### Production avec Docker Compose

1. **Configurer les secrets** dans le dossier `secrets/`
2. **Configurer l'environnement** dans `.env`
3. **Démarrer les services** :

```bash
docker compose up -d
```

### Services Déployés

| Service | Port | Description |
|---------|------|-------------|
| Traefik | 80, 443 | Reverse proxy |
| Backend | 8080 | API REST |
| Frontend | 80 | Application web |
| PostgreSQL | 5432 | Base de données |

## Documentation

- [📋 PRD (Product Requirements)](docs/prd.md)
- [🏗️ Architecture Technique](docs/architecture.md)
- [📚 Informations Globales](docs/global-info.md)

## Sécurité

- **Chiffrement des messages** : AES-256-GCM avec dérivation de clé per-user (HKDF-SHA256)
- **Isolation cryptographique** : Chaque utilisateur possède une clé unique dérivée
- **Authentification** : JWT avec signature RSA
- **Transport** : TLS 1.3 via Traefik
- **Secrets** : Gestion via Docker Secrets

En cas de détresse mentale, Serenia redirige automatiquement vers le **3114** (numéro national de prévention du suicide).

## Licence

Ce projet est propriétaire. Tous droits réservés.

---

<p align="center">
  Développé avec ❤️ par l'équipe Serenia
</p>

