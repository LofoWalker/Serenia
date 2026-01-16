# Informations Globales — Serenia

## 1. Présentation du Projet

**Serenia** est une application de chat conversationnel basée sur l'intelligence artificielle, offrant une expérience unique d'échange avec une IA au caractère authentique et décontracté.

### Identité

| Attribut | Valeur |
|----------|--------|
| **Nom** | Serenia |
| **Type** | Application Web SaaS |
| **Modèle** | Freemium (Free / Plus / Max) |
| **Langue principale** | Français |

### Philosophie

> *"Un lieu sûr pour te confier, discuter, et respirer."*

Serenia se distingue des assistants IA traditionnels par sa personnalité : celle d'un ami proche, naturel, parfois sarcastique, jamais professionnel. L'IA répond comme par SMS, en messages courts (max 180 caractères), avec un ton détendu et authentique.

---

## 2. Stack Technologique

### Backend

| Technologie | Version | Rôle |
|-------------|---------|------|
| Java | 21 LTS | Langage principal |
| Quarkus | 3.29.x | Framework applicatif |
| Hibernate Panache | - | ORM |
| Liquibase | - | Migrations BDD |
| SmallRye JWT | - | Authentification |
| SmallRye OpenAPI | - | Documentation API |
| MapStruct | 1.5.5 | Mapping DTO ↔ Entity |
| Lombok | 1.18.30 | Réduction boilerplate |

### Frontend

| Technologie | Version | Rôle |
|-------------|---------|------|
| Angular | 21.x | Framework SPA |
| TailwindCSS | 4.x | Styling |
| TypeScript | 5.9.x | Langage |
| RxJS | 7.8.x | Programmation réactive |
| Vitest | 4.x | Tests unitaires |

### Infrastructure

| Technologie | Version | Rôle |
|-------------|---------|------|
| PostgreSQL | 16 Alpine | Base de données |
| Docker | - | Containerisation |
| Traefik | 3.6.x | Reverse proxy + TLS |
| Nginx | Alpine | Serveur frontend |

### Services Externes

| Service | Usage |
|---------|-------|
| OpenAI API | Génération des réponses IA |
| Stripe | Gestion des paiements |
| SMTP | Envoi d'emails (vérification, reset) |

---

## 3. Variables d'Environnement

### Application

| Variable | Description | Défaut |
|----------|-------------|--------|
| `QUARKUS_HTTP_PORT` | Port du serveur | `8080` |
| `SERENIA_URL` | URL de l'API | `http://localhost:8080` |
| `SERENIA_FRONT_URL` | URL du frontend | `http://localhost:4200` |
| `SERENIA_ROOT_PATH` | Root path API | `` |

### Base de Données

| Variable | Description |
|----------|-------------|
| `QUARKUS_DATASOURCE_JDBC_URL` | URL JDBC PostgreSQL |
| `QUARKUS_DATASOURCE_USERNAME` | Utilisateur BDD |
| `QUARKUS_DATASOURCE_PASSWORD` | Mot de passe BDD |

### Sécurité

| Variable | Description |
|----------|-------------|
| `MP_JWT_VERIFY_PUBLICKEY_LOCATION` | Chemin clé publique JWT |
| `SMALLRYE_JWT_SIGN_KEY_LOCATION` | Chemin clé privée JWT |
| `SERENIA_SECURITY_KEY` | Clé de chiffrement AES |
| `SERENIA_AUTH_EXPIRATION_TIME` | Durée validité JWT |
| `SERENIA_AUTH_MAX_USERS` | Limite utilisateurs |

### OpenAI

| Variable | Description |
|----------|-------------|
| `OPENAI_API_KEY` | Clé API OpenAI |
| `OPENAI_MODEL` | Modèle à utiliser |

### Stripe

| Variable | Description | Défaut |
|----------|-------------|--------|
| `STRIPE_SECRET_KEY` | Clé secrète Stripe | - |
| `STRIPE_WEBHOOK_SECRET` | Secret webhook | - |
| `STRIPE_SUCCESS_URL` | URL succès paiement | `http://localhost:4200/profile?payment=success` |
| `STRIPE_CANCEL_URL` | URL annulation | `http://localhost:4200/profile?payment=cancel` |

### Email

| Variable | Description | Défaut |
|----------|-------------|--------|
| `QUARKUS_MAILER_HOST` | Serveur SMTP | `localhost` |
| `QUARKUS_MAILER_PORT` | Port SMTP | `1025` |
| `QUARKUS_MAILER_FROM` | Adresse expéditeur | `noreply@serenia.local` |

### Quotas

| Variable | Description |
|----------|-------------|
| `SERENIA_TOKENS_INPUT_LIMIT_DEFAULT` | Limite tokens input |
| `SERENIA_TOKENS_OUTPUT_LIMIT_DEFAULT` | Limite tokens output |
| `SERENIA_TOKENS_TOTAL_LIMIT_DEFAULT` | Limite tokens totale |

---

## 4. Structure du Projet

```
Serenia/
├── compose.yaml              # Docker Compose production
├── docs/                     # Documentation
│   ├── prd.md               # Product Requirements
│   ├── architecture.md      # Architecture technique
│   └── global-info.md       # Ce fichier
├── backend/
│   ├── Dockerfile           # Image backend
│   ├── pom.xml              # Dépendances Maven
│   ├── keys/                # Clés JWT (dev)
│   └── src/
│       ├── main/
│       │   ├── java/        # Code source
│       │   └── resources/   # Configuration
│       └── test/            # Tests
├── frontend/
│   ├── Dockerfile           # Image frontend
│   ├── package.json         # Dépendances npm
│   ├── angular.json         # Configuration Angular
│   └── src/                 # Code source
└── traefik/                 # Configuration Traefik
```

---

## 5. Plans d'Abonnement

| Plan | Messages/Jour | Tokens/Mois | Prix |
|------|--------------|-------------|------|
| **FREE** | Limité | Limité | Gratuit |
| **PLUS** | Étendu | Étendu | Premium |
| **MAX** | Maximum | Maximum | Premium+ |

---

## 6. Personnalité de l'IA

### Caractéristiques

- **Style** : SMS / texto uniquement
- **Longueur** : Max 180 caractères par message
- **Ton** : Détendu, naturel, parfois sarcastique
- **Interdit** : Points d'exclamation, ton "service client"

### Comportements Spéciaux

- Avis personnels assumés
- Ignorance admise sans excuses
- Refus sec pour demandes dangereuses
- Empathie réelle + orientation 3114 en cas de détresse

---

## 7. Sécurité

### Chiffrement

| Élément | Algorithme | Détails |
|---------|------------|---------|
| Messages | AES-256-GCM + HKDF | Clé dérivée par utilisateur via HKDF-SHA256 |
| JWT | RS256 (RSA 2048) | Signature asymétrique |
| Transport | TLS 1.3 | Via Traefik |

### Chiffrement per-user HKDF

Chaque utilisateur dispose d'une clé de chiffrement unique, dérivée de manière déterministe :

```
UserKey = HKDF-SHA256(MasterKey, UserID, "serenia-user-encryption-v1")
```

**Avantages :**
- Isolation cryptographique entre utilisateurs
- Aucun stockage de clé supplémentaire
- Rétrocompatibilité avec les messages existants
- Performance optimale (~1μs par dérivation)

**Format payload v1 :** `[Version: 0x01][IV: 12 bytes][Ciphertext + Auth Tag]`

### Gestion des Secrets (Production)

Les secrets sont gérés via Docker Secrets :

- `db_password`
- `jwt_private_key.pem`
- `jwt_public_key.pem`
- `security_key`
- `openai_api_key`
- `stripe_secret_key`
- `stripe_webhook_secret`
- `smtp_password`

---

## 8. Endpoints API Principaux

### Authentification

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/auth/login` | Connexion |
| POST | `/auth/register` | Inscription |
| POST | `/auth/password/forgot` | Mot de passe oublié |
| POST | `/auth/password/reset` | Réinitialisation |

### Conversation

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/conversation` | Liste des conversations |
| POST | `/conversation/message` | Envoyer un message |
| GET | `/conversation/{id}` | Détails conversation |

### Abonnement

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/subscription/status` | Statut actuel |
| POST | `/subscription/checkout` | Créer checkout Stripe |
| POST | `/subscription/portal` | Accès portail client |

### Profil

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/profile` | Obtenir profil |
| PUT | `/profile` | Modifier profil |

---

## 9. Commandes Utiles

### Développement Backend

```bash
cd backend

# Démarrer en mode dev
./mvnw quarkus:dev

# Tests
./mvnw test

# Build
./mvnw package
```

### Développement Frontend

```bash
cd frontend

# Installer les dépendances
npm install

# Démarrer en mode dev
npm start

# Build
npm run build

# Tests
npm test
```

### Docker

```bash
# Démarrer tous les services
docker compose up -d

# Voir les logs
docker compose logs -f

# Arrêter
docker compose down
```

---

## 10. Contact & Support

Pour toute question technique, consulter la documentation ou contacter l'équipe de développement.

---

*Dernière mise à jour : Janvier 2026*

