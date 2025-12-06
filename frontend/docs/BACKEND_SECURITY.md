# Serenia Backend – Sécurité & Authentification

Ce document regroupe les informations de sécurité nécessaires à l’intégration frontend :
- mécanisme d’authentification (JWT),
- headers à utiliser,
- gestion de l’expiration,
- codes d’erreur liés à la sécurité,
- considérations CORS.

---

## 1. Authentification JWT

### 1.1. Principe général

- L’authentification repose sur un **JWT** (JSON Web Token) émis par le backend lors du login (`POST /api/auth/login`).
- Le JWT est retourné dans le champ `token` du `AuthResponseDTO`.
- Les endpoints protégés sont annotés côté backend (ex. `@Authenticated`) et exigent la présence d’un header `Authorization` contenant ce token.

### 1.2. Flux d’authentification

1. L’utilisateur s’authentifie via `POST /api/auth/login` avec un `LoginRequestDTO` valide.
2. En cas de succès, le backend renvoie un `AuthResponseDTO` :
   - `user` : `UserResponseDTO` (profil utilisateur),
   - `token` : JWT.
3. Le front stocke ce token de manière sécurisée.
4. Pour chaque appel à un endpoint protégé, le front envoie :
   - `Authorization: Bearer <jwt-token>`.
5. Le backend valide le token (signature, expiration, etc.) et attache l’identité utilisateur au contexte de sécurité.

### 1.3. Claims du JWT (indications)

- `sub` : identifiant unique de l’utilisateur (UUID), utilisé côté backend pour retrouver l’utilisateur.
- D’autres claims peuvent être présents (email, rôles, dates d’expiration), selon la configuration.

### 1.4. Endpoints protégés

Le header `Authorization: Bearer <jwt>` est requis pour :

- `GET /api/auth/me`
- `DELETE /api/auth/me`
- `POST /api/conversations/add-message`
- `GET /api/conversations/{conversationId}/messages`

---

## 2. Gestion de l’expiration et des erreurs liées à l’authentification

### 2.1. JWT expiré ou invalide

- Si le token est expiré, mal formé ou non signé correctement, le backend renvoie généralement :
  - `401 UNAUTHORIZED`.
- Le front doit alors :
  - supprimer le token local (logout logique),
  - rediriger l’utilisateur vers la page de login,
  - afficher un message indiquant que la session a expiré ou que l’authentification est requise.

### 2.2. Droits insuffisants / ressources interdites

- Si l’utilisateur authentifié tente d’accéder à une ressource qui ne lui appartient pas (ex. conversation d’un autre utilisateur) :
  - le backend renvoie `403 FORBIDDEN`.
- Le front doit :
  - ne pas essayer de réessayer avec le même token,
  - afficher un message d’erreur adapté (ex. "Vous n’êtes pas autorisé à accéder à cette ressource").

### 2.3. Résumé des codes liés à la sécurité

- `401 UNAUTHORIZED` :
  - JWT manquant,
  - JWT invalide,
  - JWT expiré.
- `403 FORBIDDEN` :
  - utilisateur authentifié mais non autorisé à accéder à la ressource demandée.

---

## 3. Headers & conventions techniques

### 3.1. Headers à utiliser côté front

Pour tous les appels JSON :

- `Content-Type: application/json`
- `Accept: application/json`

Pour les endpoints protégés :

- `Authorization: Bearer <jwt-token>`

### 3.2. Base URL & préfixe

- Base URL locale (exemple) : `http://localhost:8080`
- Préfixe d’API : `/api`

Exemples :

- `POST http://localhost:8080/api/auth/login`
- `GET http://localhost:8080/api/auth/me`
- `POST http://localhost:8080/api/conversations/add-message`

---

## 4. CORS

- Le backend est configuré pour autoriser le frontend (origin) à appeler les endpoints via CORS.
- En cas d’erreurs CORS (par ex. blocage navigateur sur les requêtes cross-origin) :
  - vérifier que l’origin du frontend est bien autorisé dans la configuration serveur,
  - remonter l’information à l’équipe backend pour ajuster la configuration.

---

## 5. Bonnes pratiques côté frontend

- Toujours envoyer le header `Authorization` pour les endpoints protégés dès qu’un token valide est disponible.
- Ne **jamais** stocker le token JWT dans un emplacement non sécurisé (éviter `localStorage` si possible, privilégier mémoire, cookies sécurisés, etc., selon les contraintes du projet).
- À la réception d’un `401` :
  - considérer l’utilisateur comme déconnecté,
  - vider les données sensibles en mémoire,
  - rediriger vers la page de login.
- À la réception d’un `403` :
  - afficher un message indiquant un manque de droits,
  - éviter de boucler sur des tentatives d’accès.

