# 🌿 Serenia

**Serenia** est une application de chat empathique alimentée par l'IA (OpenAI GPT). Elle se compose d'un backend Quarkus, d'un frontend Angular et d'une base de données PostgreSQL.

---

## 📋 Table des matières

- [Architecture](#-architecture)
- [Technologies](#-technologies)
- [Prérequis](#-prérequis)
- [Configuration](#-configuration)
- [Démarrage rapide](#-démarrage-rapide)
  - [Développement local](#développement-local)
  - [Production avec Docker](#production-avec-docker)
- [Structure du projet](#-structure-du-projet)
- [API Backend](#-api-backend)
- [Variables d'environnement](#-variables-denvironnement)
- [Sécurité](#-sécurité)
- [Base de données](#-base-de-données)
- [Tests](#-tests)

---

## 🏗 Architecture

```
┌─────────────────┐     HTTP/REST      ┌─────────────────┐      JDBC       ┌─────────────────┐
│                 │ ◄─────────────────► │                 │ ◄─────────────► │                 │
│    Frontend     │     Port 80/4200    │     Backend     │    Port 5432    │   PostgreSQL    │
│    (Angular)    │                     │    (Quarkus)    │                 │                 │
│                 │                     │   Port 8087     │                 │                 │
└─────────────────┘                     └────────┬────────┘                 └─────────────────┘
                                                 │
                                                 │ HTTPS
                                                 ▼
                                        ┌─────────────────┐
                                        │     OpenAI      │
                                        │    (GPT API)    │
                                        └─────────────────┘
```

### Flux de communication

1. **Frontend → Backend** : Le frontend Angular communique avec le backend via des appels REST sur `/api/*`. En développement, l'API est accessible sur `http://localhost:8087/api`. En production avec Docker, Nginx peut servir de reverse proxy.

2. **Backend → Base de données** : Le backend Quarkus utilise Hibernate ORM avec Panache pour interagir avec PostgreSQL. Les migrations sont gérées automatiquement par Liquibase au démarrage.

3. **Backend → OpenAI** : Pour générer les réponses de l'assistant Serenia, le backend communique avec l'API OpenAI (GPT-4o-mini par défaut).

---

## 🛠 Technologies

### Backend
| Technologie | Version | Description |
|------------|---------|-------------|
| **Java** | 21 | Langage de programmation |
| **Quarkus** | 3.29.2 | Framework Java cloud-native |
| **Hibernate ORM Panache** | - | ORM et gestion des données |
| **PostgreSQL** | 16 | Base de données relationnelle |
| **Liquibase** | - | Migrations de base de données |
| **SmallRye JWT** | - | Authentification JWT |
| **OpenAI Java SDK** | 4.7.1 | Intégration API OpenAI |
| **MapStruct** | 1.5.5 | Mapping DTO/Entity |
| **Lombok** | 1.18.30 | Réduction du code boilerplate |

### Frontend
| Technologie | Version | Description |
|------------|---------|-------------|
| **Angular** | 21 | Framework frontend |
| **TypeScript** | 5.9 | Langage de programmation |
| **TailwindCSS** | 4.1 | Framework CSS utilitaire |
| **RxJS** | 7.8 | Programmation réactive |

### Infrastructure
| Technologie | Description |
|------------|-------------|
| **Docker** | Conteneurisation |
| **Nginx** | Serveur web pour le frontend |
| **Docker Compose** | Orchestration des conteneurs |

---

## 📦 Prérequis

### Développement local

- **Java 21** (JDK 21)
- **Maven 3.9+** ou utiliser le wrapper `./mvnw`
- **Node.js 22+** et **npm 11+**
- **Docker** et **Docker Compose** (pour PostgreSQL et MailHog)

### Production

- **Docker** et **Docker Compose**

---

## ⚙ Configuration

### 1. Clés JWT

Les clés RSA pour la signature JWT doivent être générées. Un script est fourni :

```bash
cd backend/src/main/resources/script
./generateKey.sh
```

Ce script génère les fichiers suivants dans un dossier `keys/` :
- `rsaPrivateKey.pem` - Clé privée RSA
- `publicKey.pem` - Clé publique
- `privateKey.pem` - Clé privée au format PKCS#8

**Copiez** ces fichiers dans :
- `backend/keys/` (pour le runtime)
- `backend/src/test/resources/keys/` (pour les tests)

### 2. Variables d'environnement

Créez un fichier `.env` à la racine du projet pour la production :

```env
# Base de données
QUARKUS_DATASOURCE_PASSWORD=votre_mot_de_passe_securise
QUARKUS_DATASOURCE_USERNAME=serenia
POSTGRES_DB=serenia

# Email
QUARKUS_MAILER_FROM=noreply@serenia.app
QUARKUS_MAILER_HOST=smtp.votreserveur.com
QUARKUS_MAILER_PORT=587
QUARKUS_MAILER_USERNAME=votre_username
QUARKUS_MAILER_PASSWORD=votre_mot_de_passe

# Sécurité
SERENIA_SECURITY_KEY=votre_cle_de_chiffrement_32_caracteres

# OpenAI
OPENAI_API_KEY=sk-votre-cle-api-openai

# URLs
SERENIA_URL=http://localhost:8087
SERENIA_FRONT_URL=http://localhost:80
```

---

## 🚀 Démarrage rapide

### Développement local

#### 1. Démarrer les services de support (PostgreSQL + MailHog)

```bash
cd backend
docker-compose up -d
```

Cela démarre :
- **PostgreSQL** sur le port `5432` (user: serenia, password: serenia, db: serenia)
- **MailHog** sur le port `1025` (SMTP) et `8025` (interface web)

#### 2. Démarrer le backend Quarkus

```bash
cd backend

# Configurer les variables d'environnement pour le développement
export QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:5432/serenia
export QUARKUS_DATASOURCE_USERNAME=serenia
export QUARKUS_DATASOURCE_PASSWORD=serenia
export MP_JWT_VERIFY_PUBLICKEY_LOCATION=file:./keys/publicKey.pem
export SMALLRYE_JWT_SIGN_KEY_LOCATION=file:./keys/privateKey.pem
export SERENIA_SECURITY_KEY=dev_security_key_32_chars_min!
export OPENAI_API_KEY=sk-votre-cle-api
export OPENAI_MODEL=gpt-4o-mini
export QUARKUS_MAILER_HOST=localhost
export QUARKUS_MAILER_PORT=1025
export QUARKUS_MAILER_FROM=noreply@serenia.local
export SERENIA_AUTH_EXPIRATION_TIME=3600
export SERENIA_AUTH_MAX_USERS=200
export SERENIA_TOKENS_INPUT_LIMIT_DEFAULT=100000
export SERENIA_TOKENS_OUTPUT_LIMIT_DEFAULT=100000
export SERENIA_TOKENS_TOTAL_LIMIT_DEFAULT=200000
export SERENIA_URL=http://localhost:8087
export SERENIA_SYSTEM_PROMPT="Tu es Serenia, une présence chaleureuse et empathique."

# Démarrer en mode développement (hot reload)
./mvnw quarkus:dev
```

Le backend sera accessible sur `http://localhost:8080` (ou le port configuré).

#### 3. Démarrer le frontend Angular

```bash
cd frontend

# Installer les dépendances
npm install

# Démarrer le serveur de développement
npm start
```

Le frontend sera accessible sur `http://localhost:4200`.

### Production avec Docker

Lancer l'ensemble de la stack en une commande :

```bash
# À la racine du projet
docker compose up -d --build
```

Cette commande :
1. Construit l'image backend (native ou JVM selon le Dockerfile configuré)
2. Construit l'image frontend (Angular + Nginx)
3. Démarre PostgreSQL
4. Lance tous les services avec les health checks appropriés

**Services disponibles :**
- Frontend : `http://localhost:80`
- Backend API : `http://localhost:8087/api`
- Health Check : `http://localhost:8087/q/health`
- OpenAPI/Swagger : `http://localhost:8087/q/openapi`

---

## 📁 Structure du projet

```
Serenia/
├── compose.yaml              # Docker Compose production (stack complète)
├── README.md
│
├── backend/
│   ├── docker-compose.yaml   # Docker Compose dev (PostgreSQL + MailHog)
│   ├── Dockerfile.jvm        # Image Docker JVM
│   ├── Dockerfile.native     # Image Docker Native (GraalVM)
│   ├── pom.xml
│   ├── mvnw                  # Maven Wrapper
│   ├── keys/                 # Clés JWT (à générer)
│   │   ├── privateKey.pem
│   │   ├── publicKey.pem
│   │   └── rsaPrivateKey.pem
│   └── src/
│       ├── main/
│       │   ├── java/com/lofo/serenia/
│       │   │   ├── config/       # Configuration (CORS, OpenAI, etc.)
│       │   │   ├── domain/       # Entités JPA
│       │   │   ├── dto/          # Objets de transfert (in/out)
│       │   │   ├── exception/    # Exceptions métier
│       │   │   ├── mapper/       # MapStruct mappers
│       │   │   ├── repository/   # Repositories Panache
│       │   │   ├── resource/     # Endpoints REST (JAX-RS)
│       │   │   └── service/      # Logique métier
│       │   └── resources/
│       │       ├── application.properties
│       │       └── db/changelog/ # Migrations Liquibase
│       └── test/
│
└── frontend/
    ├── Dockerfile            # Build Angular + Nginx
    ├── nginx.conf            # Configuration Nginx
    ├── package.json
    ├── angular.json
    └── src/
        ├── app/
        │   ├── core/         # Guards, interceptors, services globaux
        │   ├── features/     # Modules fonctionnels
        │   │   ├── auth/     # Login, register, activation
        │   │   ├── chat/     # Interface de conversation
        │   │   ├── home/     # Page d'accueil
        │   │   └── profile/  # Profil utilisateur
        │   └── shared/       # Composants et layouts partagés
        └── environments/     # Configuration par environnement
```

---

## 🔌 API Backend

### Endpoints d'authentification (`/api/auth`)

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| `POST` | `/api/auth/register` | Inscription utilisateur | ❌ |
| `GET` | `/api/auth/activate?token=xxx` | Activation du compte | ❌ |
| `POST` | `/api/auth/login` | Connexion | ❌ |
| `GET` | `/api/auth/me` | Infos utilisateur connecté | ✅ |
| `DELETE` | `/api/auth/me` | Suppression du compte | ✅ |

### Endpoints de conversation (`/api/conversations`)

| Méthode | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| `POST` | `/api/conversations/add-message` | Envoyer un message | ✅ |
| `GET` | `/api/conversations/{id}/messages` | Messages d'une conversation | ✅ |
| `GET` | `/api/conversations/my-messages` | Messages de l'utilisateur | ✅ |

### Documentation OpenAPI

Accessible en développement sur : `http://localhost:8087/q/openapi`

---

## 🔐 Variables d'environnement

### Variables obligatoires

| Variable | Description | Exemple |
|----------|-------------|---------|
| `QUARKUS_DATASOURCE_PASSWORD` | Mot de passe PostgreSQL | `secret123` |
| `QUARKUS_MAILER_FROM` | Adresse email expéditeur | `noreply@serenia.app` |
| `QUARKUS_MAILER_HOST` | Serveur SMTP | `smtp.gmail.com` |
| `QUARKUS_MAILER_USERNAME` | Utilisateur SMTP | `user@gmail.com` |
| `QUARKUS_MAILER_PASSWORD` | Mot de passe SMTP | `app-password` |
| `SERENIA_SECURITY_KEY` | Clé de chiffrement (min 32 car.) | `my_super_secret_key_32_chars!!` |
| `OPENAI_API_KEY` | Clé API OpenAI | `sk-...` |

### Variables optionnelles (avec valeurs par défaut)

| Variable | Défaut | Description |
|----------|--------|-------------|
| `QUARKUS_HTTP_PORT` | `8087` | Port du backend |
| `QUARKUS_DATASOURCE_JDBC_URL` | `jdbc:postgresql://postgres:5432/serenia` | URL JDBC |
| `QUARKUS_DATASOURCE_USERNAME` | `serenia` | Utilisateur DB |
| `OPENAI_MODEL` | `gpt-4o-mini` | Modèle OpenAI |
| `SERENIA_AUTH_EXPIRATION_TIME` | `3600` | Durée du token JWT (secondes) |
| `SERENIA_AUTH_MAX_USERS` | `200` | Nombre max d'utilisateurs |
| `CORS_ORIGINS` | `http://localhost:4200` | Origines CORS autorisées |

---

## 🔒 Sécurité

### Authentification JWT

- Les tokens JWT sont signés avec RS256 (clés RSA)
- Durée de vie configurable via `SERENIA_AUTH_EXPIRATION_TIME`
- Les clés doivent être générées et stockées de manière sécurisée

### Chiffrement des données

- Les messages de conversation sont chiffrés en base de données
- La clé de chiffrement est définie par `SERENIA_SECURITY_KEY`

### CORS

- Configuré pour accepter les requêtes du frontend
- Les origines autorisées sont définies par `CORS_ORIGINS`

### Headers de sécurité (Nginx)

Le frontend en production inclut les headers suivants :
- `X-Frame-Options: SAMEORIGIN`
- `X-Content-Type-Options: nosniff`
- `X-XSS-Protection: 1; mode=block`
- `Content-Security-Policy`

---

## 🗄 Base de données

### Schéma

Les migrations Liquibase sont exécutées automatiquement au démarrage. Les fichiers de migration se trouvent dans :
`backend/src/main/resources/db/changelog/`

Migrations disponibles :
1. `01-create-tables.yaml` - Tables principales (users, conversations, messages)
2. `02-insert-default-roles.yaml` - Rôles par défaut
3. `03-create-token-tables.yaml` - Gestion des quotas de tokens
4. `04-add-email-verification-columns.yaml` - Vérification email

### Connexion directe

```bash
# Via Docker
docker exec -it serenia-postgres psql -U serenia -d serenia

# Ou directement
psql -h localhost -U serenia -d serenia
```

---

## 🧪 Tests

### Backend

```bash
cd backend

# Exécuter tous les tests
./mvnw test

# Exécuter avec couverture
./mvnw test jacoco:report
```

### Frontend

```bash
cd frontend

# Exécuter les tests unitaires (Vitest)
npm test
```

---

## 📝 Commandes utiles

```bash
# Voir les logs des conteneurs
docker compose logs -f

# Voir les logs d'un service spécifique
docker compose logs -f backend

# Reconstruire un service
docker compose up -d --build backend

# Arrêter tous les services
docker compose down

# Arrêter et supprimer les volumes (reset complet)
docker compose down -v

# Vérifier la santé des services
curl http://localhost:8087/q/health

# Compiler le backend en natif (local)
cd backend && ./mvnw package -Pnative
```

---

## 📄 Licence

Projet privé - Tous droits réservés.

---

## 👥 Contributeurs

- **Lofo** - Développeur principal

