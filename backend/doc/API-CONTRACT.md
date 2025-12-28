# Serenia API - Contrat d'Interface

> **Version:** 1.0.0  
> **Base URL:** `https://api.serenia.app` (Production) | `http://localhost:8081` (Développement)  
> **Content-Type:** `application/json`  
> **Date:** 2025-12-28

---

## Table des matières

1. [Authentification](#1-authentification)
2. [Inscription & Activation](#2-inscription--activation)
3. [Gestion du mot de passe](#3-gestion-du-mot-de-passe)
4. [Profil utilisateur](#4-profil-utilisateur)
5. [Conversations](#5-conversations)
6. [Abonnements & Quotas](#6-abonnements--quotas)
7. [Modèles de données](#7-modèles-de-données)
8. [Codes d'erreur](#8-codes-derreur)

---

## 1. Authentification

### POST `/api/auth/login`

Authentifie un utilisateur et retourne un token JWT.

#### Request

```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

| Champ      | Type   | Requis | Validation                    |
|------------|--------|--------|-------------------------------|
| `email`    | string | ✅     | Format email valide           |
| `password` | string | ✅     | Non vide                      |

#### Responses

**200 OK** - Authentification réussie
```json
{
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "lastName": "Doe",
    "firstName": "John",
    "email": "user@example.com",
    "role": "USER"
  },
  "token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**400 Bad Request** - Payload invalide
```json
{
  "id": "...",
  "timestamp": 1703779200000,
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Email is required",
  "path": "/api/auth/login",
  "traceId": "..."
}
```

**401 Unauthorized** - Identifiants invalides ou compte non activé
```json
{
  "message": "Email ou mot de passe invalide"
}
```

---

## 2. Inscription & Activation

### POST `/api/auth/register`

Crée un nouveau compte utilisateur et envoie un email d'activation.

#### Request

```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane@example.com",
  "password": "SecurePassword123!"
}
```

| Champ       | Type   | Requis | Validation                           |
|-------------|--------|--------|--------------------------------------|
| `firstName` | string | ✅     | Non vide                             |
| `lastName`  | string | ✅     | Non vide                             |
| `email`     | string | ✅     | Format email valide, unique          |
| `password`  | string | ✅     | Minimum 6 caractères                 |

#### Responses

**201 Created** - Inscription réussie
```json
{
  "message": "Inscription réussie. Veuillez vérifier votre email pour activer votre compte."
}
```

**400 Bad Request** - Email déjà utilisé ou validation échouée
```json
{
  "message": "Email already exists"
}
```

---

### GET `/api/auth/activate`

Active un compte utilisateur via le token reçu par email.

#### Query Parameters

| Paramètre | Type   | Requis | Description                          |
|-----------|--------|--------|--------------------------------------|
| `token`   | string | ✅     | Token d'activation reçu par email    |

#### Responses

**200 OK** - Compte activé
```json
{
  "message": "Compte activé avec succès"
}
```

**400 Bad Request** - Token manquant, vide ou invalide
```json
{
  "message": "Token is required"
}
```

---

## 3. Gestion du mot de passe

### POST `/api/password/forgot`

Demande l'envoi d'un email de réinitialisation de mot de passe.

> ⚠️ **Sécurité:** Retourne toujours 200 pour éviter l'énumération des utilisateurs.

#### Request

```json
{
  "email": "user@example.com"
}
```

| Champ   | Type   | Requis | Validation          |
|---------|--------|--------|---------------------|
| `email` | string | ✅     | Format email valide |

#### Responses

**200 OK** - Demande traitée (que l'email existe ou non)
```json
{
  "message": "Si un compte existe avec cet email, un lien de réinitialisation a été envoyé."
}
```

**400 Bad Request** - Payload invalide
```json
{
  "message": "Email is required"
}
```

---

### POST `/api/password/reset`

Réinitialise le mot de passe avec un token valide.

#### Request

```json
{
  "token": "reset-token-from-email",
  "newPassword": "NewSecurePassword456!"
}
```

| Champ         | Type   | Requis | Validation              |
|---------------|--------|--------|-------------------------|
| `token`       | string | ✅     | Non vide                |
| `newPassword` | string | ✅     | Minimum 8 caractères    |

#### Responses

**200 OK** - Mot de passe réinitialisé
```json
{
  "message": "Mot de passe réinitialisé avec succès"
}
```

**400 Bad Request** - Validation échouée
```json
{
  "message": "Password must be at least 8 characters"
}
```

**401 Unauthorized** - Token invalide ou expiré
```json
{
  "message": "Token invalide ou expiré"
}
```

---

## 4. Profil utilisateur

> 🔒 **Authentification requise:** Header `Authorization: Bearer <token>`

### GET `/api/profile`

Récupère le profil de l'utilisateur connecté.

#### Responses

**200 OK** - Profil retourné
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "lastName": "Doe",
  "firstName": "John",
  "email": "user@example.com",
  "role": "USER"
}
```

**401 Unauthorized** - Non authentifié ou token invalide
```json
{
  "message": "Utilisateur non authentifié"
}
```

---

### DELETE `/api/profile`

Supprime définitivement le compte de l'utilisateur connecté.

#### Responses

**204 No Content** - Compte supprimé

**401 Unauthorized** - Non authentifié
```json
{
  "message": "Utilisateur non authentifié"
}
```

---

## 5. Conversations

> 🔒 **Authentification requise:** Header `Authorization: Bearer <token>`

### POST `/api/conversations/add-message`

Envoie un message et reçoit la réponse de l'assistant IA.

#### Request

```json
{
  "content": "Bonjour, comment puis-je gérer mon stress ?"
}
```

| Champ     | Type   | Requis | Validation |
|-----------|--------|--------|------------|
| `content` | string | ✅     | Non vide   |

#### Responses

**200 OK** - Message traité
```json
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440000",
  "role": "ASSISTANT",
  "content": "Je comprends que le stress peut être difficile à gérer..."
}
```

**400 Bad Request** - Contenu vide
```json
{
  "message": "Content is required"
}
```

**401 Unauthorized** - Non authentifié

**429 Too Many Requests** - Quota dépassé
```json
{
  "quotaType": "DAILY_MESSAGE_LIMIT",
  "limit": 10,
  "current": 10,
  "requested": 1,
  "message": "Limite quotidienne de messages atteinte"
}
```

---

### GET `/api/conversations/my-messages`

Récupère tous les messages de la conversation active de l'utilisateur.

#### Responses

**200 OK** - Messages retournés
```json
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440000",
  "messages": [
    {
      "role": "USER",
      "content": "Bonjour"
    },
    {
      "role": "ASSISTANT",
      "content": "Bonjour ! Comment puis-je vous aider ?"
    }
  ]
}
```

**204 No Content** - Aucune conversation active

**401 Unauthorized** - Non authentifié

---

### GET `/api/conversations/{conversationId}/messages`

Récupère les messages d'une conversation spécifique.

#### Path Parameters

| Paramètre        | Type | Description               |
|------------------|------|---------------------------|
| `conversationId` | UUID | ID de la conversation     |

#### Responses

**200 OK** - Messages retournés
```json
{
  "conversationId": "550e8400-e29b-41d4-a716-446655440000",
  "messages": [...]
}
```

**401 Unauthorized** - Non authentifié

**403 Forbidden** - Accès non autorisé à cette conversation

**404 Not Found** - Conversation inexistante

---

### DELETE `/api/conversations/my-conversations`

Supprime toutes les conversations de l'utilisateur.

#### Responses

**204 No Content** - Conversations supprimées

**401 Unauthorized** - Non authentifié

---

## 6. Abonnements & Quotas

> 🔒 **Authentification requise:** Header `Authorization: Bearer <token>`

### GET `/api/subscription/status`

Récupère le statut de l'abonnement et les quotas de l'utilisateur.

#### Responses

**200 OK** - Statut retourné
```json
{
  "planName": "FREE",
  "tokensRemainingThisMonth": 8500,
  "messagesRemainingToday": 7,
  "perMessageTokenLimit": 1000,
  "monthlyTokenLimit": 10000,
  "dailyMessageLimit": 10,
  "tokensUsedThisMonth": 1500,
  "messagesSentToday": 3,
  "monthlyResetDate": "2025-01-28T10:30:00",
  "dailyResetDate": "2025-12-29T10:30:00"
}
```

| Champ                     | Type     | Description                                    |
|---------------------------|----------|------------------------------------------------|
| `planName`                | string   | Nom du plan (`FREE`, `PLUS`, `MAX`)            |
| `tokensRemainingThisMonth`| integer  | Tokens restants ce mois-ci                     |
| `messagesRemainingToday`  | integer  | Messages restants aujourd'hui                  |
| `perMessageTokenLimit`    | integer  | Limite de tokens par message                   |
| `monthlyTokenLimit`       | integer  | Limite mensuelle de tokens                     |
| `dailyMessageLimit`       | integer  | Limite quotidienne de messages                 |
| `tokensUsedThisMonth`     | integer  | Tokens utilisés ce mois-ci                     |
| `messagesSentToday`       | integer  | Messages envoyés aujourd'hui                   |
| `monthlyResetDate`        | datetime | Date de réinitialisation du compteur mensuel   |
| `dailyResetDate`          | datetime | Date de réinitialisation du compteur quotidien |

**401 Unauthorized** - Non authentifié

**404 Not Found** - Subscription non trouvée (erreur système)
```json
{
  "message": "Subscription not found for user: ..."
}
```

---

### PUT `/api/subscription/plan`

Change le plan d'abonnement de l'utilisateur authentifié.

#### Request

```json
{
  "planType": "PLUS"
}
```

| Champ      | Type   | Requis | Validation                      |
|------------|--------|--------|---------------------------------|
| `planType` | string | ✅     | `FREE`, `PLUS` ou `MAX`         |

#### Responses

**200 OK** - Plan changé avec succès
```json
{
  "planName": "PLUS",
  "tokensRemainingThisMonth": 99500,
  "messagesRemainingToday": 48,
  "perMessageTokenLimit": 4000,
  "monthlyTokenLimit": 100000,
  "dailyMessageLimit": 50,
  "tokensUsedThisMonth": 500,
  "messagesSentToday": 2,
  "monthlyResetDate": "2025-01-28T10:30:00",
  "dailyResetDate": "2025-12-29T10:30:00"
}
```

**400 Bad Request** - Type de plan invalide ou manquant
```json
{
  "message": "Plan type is required"
}
```

**401 Unauthorized** - Non authentifié

**404 Not Found** - Subscription ou plan non trouvé
```json
{
  "message": "Subscription not found for user: ..."
}
```

---

## 7. Modèles de données

### UserResponseDTO

```json
{
  "id": "UUID",
  "lastName": "string",
  "firstName": "string",
  "email": "string",
  "role": "string (USER | ADMIN)"
}
```

### AuthResponseDTO

```json
{
  "user": "UserResponseDTO",
  "token": "string (JWT)"
}
```

### ApiMessageResponse

```json
{
  "message": "string"
}
```

### MessageResponseDTO

```json
{
  "conversationId": "UUID",
  "role": "string (USER | ASSISTANT)",
  "content": "string"
}
```

### ConversationMessagesResponseDTO

```json
{
  "conversationId": "UUID",
  "messages": [
    {
      "role": "string (USER | ASSISTANT)",
      "content": "string"
    }
  ]
}
```

### SubscriptionStatusDTO

```json
{
  "planName": "string",
  "tokensRemainingThisMonth": "integer",
  "messagesRemainingToday": "integer",
  "perMessageTokenLimit": "integer",
  "monthlyTokenLimit": "integer",
  "dailyMessageLimit": "integer",
  "tokensUsedThisMonth": "integer",
  "messagesSentToday": "integer",
  "monthlyResetDate": "datetime (ISO 8601)",
  "dailyResetDate": "datetime (ISO 8601)"
}
```

### QuotaErrorDTO

```json
{
  "quotaType": "string (DAILY_MESSAGE_LIMIT | MONTHLY_TOKEN_LIMIT | MESSAGE_TOKEN_LIMIT)",
  "limit": "integer",
  "current": "integer",
  "requested": "integer",
  "message": "string"
}
```

### ErrorResponse

```json
{
  "id": "UUID",
  "timestamp": "long (epoch ms)",
  "status": "integer (HTTP status code)",
  "error": "string (error code)",
  "message": "string",
  "path": "string",
  "traceId": "string (nullable)"
}
```

---

## 8. Codes d'erreur

### Codes HTTP

| Code | Signification            | Usage                                        |
|------|--------------------------|----------------------------------------------|
| 200  | OK                       | Requête réussie avec contenu                 |
| 201  | Created                  | Ressource créée (inscription)                |
| 204  | No Content               | Requête réussie sans contenu (suppression)   |
| 400  | Bad Request              | Validation échouée, payload invalide         |
| 401  | Unauthorized             | Non authentifié, token invalide/expiré       |
| 403  | Forbidden                | Accès refusé (compte non activé, ressource)  |
| 404  | Not Found                | Ressource non trouvée                        |
| 409  | Conflict                 | Conflit (email déjà utilisé)                 |
| 429  | Too Many Requests        | Quota dépassé                                |
| 500  | Internal Server Error    | Erreur serveur inattendue                    |

### Codes d'erreur applicatifs

| Code                    | Description                                  |
|-------------------------|----------------------------------------------|
| `VALIDATION_ERROR`      | Erreur de validation des champs              |
| `UNAUTHORIZED`          | Authentification requise ou échouée          |
| `FORBIDDEN`             | Accès interdit                               |
| `NOT_FOUND`             | Ressource non trouvée                        |
| `CONFLICT`              | Conflit de données                           |
| `INTERNAL_SERVER_ERROR` | Erreur interne                               |
| `QUOTA_EXCEEDED`        | Quota d'utilisation dépassé                  |

### Types de quota

| Type                   | Description                                   |
|------------------------|-----------------------------------------------|
| `DAILY_MESSAGE_LIMIT`  | Limite quotidienne de messages atteinte       |
| `MONTHLY_TOKEN_LIMIT`  | Limite mensuelle de tokens atteinte           |
| `MESSAGE_TOKEN_LIMIT`  | Message trop long (dépasse la limite/message) |

---

## Authentification JWT

### Header requis

```
Authorization: Bearer <token>
```

### Structure du token

Le token JWT contient les claims suivants :

| Claim    | Description                        |
|----------|------------------------------------|
| `sub`    | UUID de l'utilisateur              |
| `upn`    | Email de l'utilisateur             |
| `groups` | Rôles de l'utilisateur (`USER`)    |
| `iat`    | Date d'émission                    |
| `exp`    | Date d'expiration                  |
| `iss`    | Émetteur (`serenia`)               |

### Durée de validité

- **Token d'accès:** 1 heure
- **Token d'activation:** 24 heures
- **Token de réinitialisation:** 1 heure

---

## Plans d'abonnement

| Plan | Tokens/mois | Messages/jour | Tokens/message |
|------|-------------|---------------|----------------|
| FREE | 10,000      | 10            | 1,000          |
| PLUS | 100,000     | 50            | 4,000          |
| MAX  | 500,000     | 200           | 8,000          |

---

## Notes d'implémentation

### Sécurité

- Les mots de passe sont hashés avec BCrypt
- Les tokens sont signés avec RS256
- L'endpoint `/api/password/forgot` ne révèle pas si l'email existe
- Les conversations sont isolées par utilisateur

### Quotas

- Les quotas sont vérifiés **avant** chaque appel à l'IA
- Les compteurs sont réinitialisés automatiquement :
  - Quotidien : 24h après le premier message du jour
  - Mensuel : 30 jours après le premier message du mois
- Une subscription FREE est créée automatiquement à l'inscription

### Idempotence

- `DELETE /api/profile` : Idempotent
- `DELETE /api/conversations/my-conversations` : Idempotent

