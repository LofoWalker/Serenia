# Serenia Backend – Contrat d’Interface (API)

Ce document décrit les endpoints exposés par le backend Serenia, les payloads attendus et retournés et les principaux codes de statut.

Les structures et règles de validation reposent sur les DTO du package `com.lofo.serenia.dto.*`.

---

## 1. Authentification (`/api/auth`)

### 1.1. Inscription – `POST /api/auth/register`

**Description**  
Créer un compte utilisateur et déclencher l’envoi d’un email d’activation.

**DTO d’entrée : `RegistrationRequestDTO`**

```java
public record RegistrationRequestDTO(
        @NotBlank(message = "Last name is required") String lastName,
        @NotBlank(message = "First name is required") String firstName,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
        @NotBlank(message = "Password is required") @Size(min = 6, message = "Password must contain at least 6 characters") String password
) {}
```

**Requête**

- URL : `/api/auth/register`
- Méthode : `POST`
- Headers :
  - `Content-Type: application/json`
  - `Accept: application/json`
- Body (JSON) :

```json
{
  "lastName": "Doe",
  "firstName": "John",
  "email": "john.doe@example.com",
  "password": "Secret1"
}
```

**DTO de sortie : `RegistrationResponseDTO`**

```java
public record RegistrationResponseDTO(String message) {}
```

**Réponses**

- `201 CREATED`
  - Exemple de body :
  ```json
  {
    "message": "Registration successful. Please check your email to activate your account."
  }
  ```
- `400 BAD REQUEST`
  - Violations de validation (champs manquants, email invalide, mot de passe trop court, etc.).

---

### 1.2. Activation de compte – `GET /api/auth/activate`

**Description**  
Activer le compte utilisateur à partir d’un token reçu par email.

**DTO de sortie : `ActivationResponseDTO`**

```java
public record ActivationResponseDTO(String message) {}
```

**Requête**

- URL : `/api/auth/activate?token=<token>`
- Méthode : `GET`
- Headers :
  - `Accept: application/json`
- Query params :
  - `token` (String) : obligatoire côté métier.

**Réponses**

- `200 OK`
  - Exemple de body :
  ```json
  {
    "message": "Account successfully activated. You can now log in."
  }
  ```
- `400 BAD REQUEST`
  - Token manquant, invalide ou expiré.

---

### 1.3. Login – `POST /api/auth/login`

**Description**  
Authentifier un utilisateur et renvoyer un JWT + les informations de profil.

**DTO d’entrée : `LoginRequestDTO`**

```java
public record LoginRequestDTO(
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") String email,
        @NotBlank(message = "Password is required") String password
) {}
```

**Requête**

- URL : `/api/auth/login`
- Méthode : `POST`
- Headers :
  - `Content-Type: application/json`
  - `Accept: application/json`
- Body (JSON) :

```json
{
  "email": "john.doe@example.com",
  "password": "Secret1"
}
```

**DTO de sortie : `AuthResponseDTO`**

```java
public record AuthResponseDTO(UserResponseDTO user, String token) {}
```

**DTO embarqué : `UserResponseDTO`**

```java
public record UserResponseDTO(
        UUID id,
        String lastName,
        String firstName,
        String email,
        Set<String> roles
) {}
```

**Réponses**

- `200 OK`
  - Exemple de body :
  ```json
  {
    "user": {
      "id": "6d5b0a18-0e1c-4f3e-b92b-123456789abc",
      "lastName": "Doe",
      "firstName": "John",
      "email": "john.doe@example.com",
      "roles": ["USER"]
    },
    "token": "<jwt-token>"
  }
  ```
- `401 UNAUTHORIZED`
  - Identifiants incorrects.
- `403 FORBIDDEN`
  - Compte non activé.

---

### 1.4. Profil courant – `GET /api/auth/me`

**Description**  
Récupérer les informations du profil de l’utilisateur actuellement authentifié.

**Requête**

- URL : `/api/auth/me`
- Méthode : `GET`
- Headers :
  - `Authorization: Bearer <jwt-token>`
  - `Accept: application/json`

**DTO de sortie : `UserResponseDTO`**

Exemple de body :

```json
{
  "id": "6d5b0a18-0e1c-4f3e-b92b-123456789abc",
  "lastName": "Doe",
  "firstName": "John",
  "email": "john.doe@example.com",
  "roles": ["USER"]
}
```

**Réponses**

- `200 OK`
- `401 UNAUTHORIZED` si le JWT est manquant ou invalide.

---

### 1.5. Suppression de compte – `DELETE /api/auth/me`

**Description**  
Supprimer le compte de l’utilisateur authentifié ainsi que ses données associées.

**Requête**

- URL : `/api/auth/me`
- Méthode : `DELETE`
- Headers :
  - `Authorization: Bearer <jwt-token>`

**Réponses**

- `204 NO CONTENT`
- `401 UNAUTHORIZED` si le JWT est manquant ou invalide.

---

## 2. Conversations & Chat (`/api/conversations`)

### 2.1. Envoyer un message – `POST /api/conversations/add-message`

**Description**  
Envoyer un message utilisateur, créer ou réutiliser une conversation et obtenir la réponse IA.

**DTO d’entrée : `MessageRequestDTO`**

```java
public record MessageRequestDTO(@NotBlank String content) {}
```

**Requête**

- URL : `/api/conversations/add-message`
- Méthode : `POST`
- Headers :
  - `Authorization: Bearer <jwt-token>`
  - `Content-Type: application/json`
  - `Accept: application/json`
- Body (JSON) :

```json
{
  "content": "Bonjour, peux-tu m’aider ?"
}
```

**DTO de sortie : `MessageResponseDTO`**

```java
public record MessageResponseDTO(
        UUID conversationId,
        MessageRole role,
        String content
) {}
```

**Exemple de réponse**

```json
{
  "conversationId": "4c0c8c4a-7c4c-4e94-a84c-123456789abc",
  "role": "assistant",
  "content": "Bonjour, bien sûr, je peux vous aider. Que souhaitez-vous savoir ?"
}
```

**Réponses**

- `200 OK`
- `400 BAD REQUEST` si `content` est manquant ou vide.
- `401 UNAUTHORIZED` si le JWT est manquant ou invalide.

---

### 2.2. Récupérer les messages d’une conversation – `GET /api/conversations/{conversationId}/messages`

**Description**  
Obtenir l’historique des messages pour une conversation appartenant à l’utilisateur authentifié.

**Requête**

- URL : `/api/conversations/{conversationId}/messages`
- Méthode : `GET`
- Headers :
  - `Authorization: Bearer <jwt-token>`
  - `Accept: application/json`
- Path parameters :
  - `conversationId` (UUID) : identifiant de la conversation.

**Exemple de réponse (liste de messages)**

```json
[
  {
    "role": "user",
    "content": "Bonjour, peux-tu m’aider ?",
    "timestamp": "2025-01-01T10:00:00Z"
  },
  {
    "role": "assistant",
    "content": "Bonjour, bien sûr, je peux vous aider. Que souhaitez-vous savoir ?",
    "timestamp": "2025-01-01T10:00:02Z"
  }
]
```

**Réponses**

- `200 OK`
- `401 UNAUTHORIZED` si le JWT est manquant ou invalide.
- `403 FORBIDDEN` si la conversation n’appartient pas à l’utilisateur authentifié.

---

## 3. Récapitulatif des DTO exposés

### 3.1. DTO d’entrée

- `RegistrationRequestDTO` : `{ lastName, firstName, email, password }`
- `LoginRequestDTO` : `{ email, password }`
- `MessageRequestDTO` : `{ content }`

### 3.2. DTO de sortie

- `RegistrationResponseDTO` : `{ message }`
- `ActivationResponseDTO` : `{ message }`
- `AuthResponseDTO` : `{ user: UserResponseDTO, token }`
- `UserResponseDTO` : `{ id, lastName, firstName, email, roles }`
- `MessageResponseDTO` : `{ conversationId, role, content }`

