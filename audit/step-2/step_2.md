# Step 2 : Vérification des Contrôles d'Accès et IDOR

## Contexte

Les vulnérabilités IDOR (Insecure Direct Object Reference) permettent à un attaquant d'accéder aux données d'autres utilisateurs en manipulant des identifiants dans les requêtes. L'application Serenia gère des conversations privées entre utilisateurs et une IA, ce qui rend l'isolation des données critique.

### État actuel identifié

L'audit préliminaire indique que :
- `ConversationRepository.findByIdAndUser()` existe et vérifie l'appartenance
- Le pattern `userId` est utilisé dans les requêtes Panache
- Un endpoint admin existe avec `@RolesAllowed("ADMIN")`

### Fichiers à auditer

| Fichier | Rôle | Risque IDOR |
|---------|------|-------------|
| `ConversationRepository.java` | Accès aux conversations | Élevé |
| `MessageRepository.java` | Accès aux messages | Élevé |
| `ConversationService.java` | Logique métier conversations | Élevé |
| `ConversationResource.java` | Endpoints REST conversations | Élevé |
| `AdminResource.java` | Endpoints administrateur | Moyen (accès aux données tous users) |
| `ProfileResource.java` | Profil utilisateur | Moyen |

---

## Objectif

1. **Valider l'isolation des données** : Confirmer que chaque requête vers les données utilisateur inclut une vérification du `userId`
2. **Auditer les endpoints REST** : S'assurer qu'aucun endpoint n'expose des données sans contrôle d'appartenance
3. **Vérifier les contrôles RBAC** : Confirmer que seuls les admins accèdent aux endpoints admin
4. **Identifier les vecteurs IDOR potentiels** : UUID prévisibles, endpoints oubliés, paramètres de requête

---

## Méthode

### 2.1 Audit du Repository Layer

#### Pattern de sécurité attendu

Chaque méthode du repository accédant à des données utilisateur doit :
1. Accepter un paramètre `userId`
2. Inclure `userId` dans la clause WHERE

**ConversationRepository.java - Analyse :**
```java
// ✅ SÉCURISÉ - Filtre par userId
public Optional<Conversation> findActiveByUser(UUID userId) {
    return find("userId = ?1", userId).firstResultOptional();
}

// ✅ SÉCURISÉ - Double vérification conversationId + userId
public Optional<Conversation> findByIdAndUser(UUID conversationId, UUID userId) {
    return find("id = ?1 and userId = ?2", conversationId, userId).firstResultOptional();
}

// ⚠️ À VÉRIFIER - Utilisé uniquement en interne ?
public Optional<Conversation> findByConversationId(UUID conversationId) {
    return find("id = ?1", conversationId).firstResultOptional();
}
```

**Points de vérification :**
- [ ] `findByConversationId()` n'est JAMAIS appelé directement depuis un controller
- [ ] Toutes les méthodes publiques du service utilisent `findByIdAndUser()`

#### Recherche de méthodes non sécurisées

```bash
# Trouver toutes les méthodes find* sans userId
grep -rn "find.*(" backend/src/main/java/com/lofo/serenia/persistence/repository/ \
  | grep -v "userId" | grep -v "email" | grep -v "test"
```

### 2.2 Audit du Service Layer

#### ConversationService - Points de contrôle

```java
// VÉRIFIER que getConversationMessages utilise findByIdAndUser
public List<ChatMessage> getConversationMessages(UUID conversationId, UUID userId) {
    Conversation conversation = conversationRepository
        .findByIdAndUser(conversationId, userId)  // ✅ Doit être cette méthode
        .orElseThrow(() -> new NotFoundException("Conversation not found"));
    // ...
}
```

**Checklist d'audit :**

| Méthode | Vérifie userId ? | Statut |
|---------|-----------------|--------|
| `getActiveConversationByUserId(userId)` | ✅ Paramètre userId | Sécurisé |
| `getConversationMessages(convId, userId)` | ✅ findByIdAndUser | À vérifier |
| `createConversation(userId)` | ✅ Paramètre userId | Sécurisé |
| `deleteConversation(convId)` | ❓ | À vérifier |

### 2.3 Audit des Endpoints REST

#### Méthodologie d'audit

Pour chaque endpoint, vérifier :
1. L'extraction du `userId` depuis le JWT (pas depuis un paramètre)
2. Le passage du `userId` aux méthodes service
3. L'absence de paramètres `userId` ou `conversationId` manipulables

**ConversationResource.java - Analyse :**

```java
// ✅ Pattern sécurisé - userId extrait du JWT
private UUID getAuthenticatedUserId() {
    return UUID.fromString(jwt.getSubject()); // Depuis le token, non manipulable
}

@POST
@Path("/add-message")
public Response addMessage(MessageRequestDTO request) {
    UUID userId = getAuthenticatedUserId();  // ✅ Depuis JWT
    // request ne contient PAS de userId     // ✅ Pas d'IDOR
    ProcessedMessageResult result = chatOrchestrator.processUserMessage(userId, request.content());
    return Response.ok(response).build();
}

@GET
@Path("/my-messages")
public Response getUserMessages() {
    UUID userId = getAuthenticatedUserId();  // ✅ Depuis JWT
    Conversation conversation = conversationService.getActiveConversationByUserId(userId);
    // ...
}
```

**❌ Patterns dangereux à rechercher :**

```java
// DANGEREUX - userId depuis le body
@POST
public Response getMessage(@RequestBody MessageRequest req) {
    return service.getMessages(req.getUserId());  // ❌ IDOR !
}

// DANGEREUX - conversationId sans vérification userId
@GET
@Path("/{conversationId}")
public Response getConversation(@PathParam("conversationId") UUID convId) {
    return service.getConversation(convId);  // ❌ Peut accéder aux conv. d'autres users
}
```

### 2.4 Audit AdminResource

```java
@Path("/admin")
@RolesAllowed("ADMIN")  // ✅ Protection RBAC
public class AdminResource {
    
    @GET
    @Path("/users")
    public Response getUsers(...) {  // ✅ Accès à tous les users = normal pour admin
        UserListDTO users = adminStatsService.getUserList(page, size);
        return Response.ok(users).build();
    }
    
    @GET
    @Path("/users/{email}")  // ⚠️ Paramètre email - vérifier pas d'injection
    public Response getUserByEmail(@PathParam("email") String email) {
        // Vérifier que email est validé/échappé
    }
}
```

**Points de vérification Admin :**
- [ ] `@RolesAllowed("ADMIN")` est au niveau classe
- [ ] Aucun endpoint admin sans annotation de sécurité
- [ ] Les paramètres sont validés (email format, injection SQL)
- [ ] Logs d'audit pour les actions admin

### 2.5 Tests IDOR Manuels

#### Scénarios de test

**Préparation :**
1. Créer 2 utilisateurs : `userA@test.com` et `userB@test.com`
2. Chacun crée une conversation avec messages
3. Récupérer les JWT des deux utilisateurs
4. Noter les `conversationId` de chaque utilisateur

**Tests à exécuter :**

```bash
# Test IDOR #1 : Accès aux messages d'un autre user
# userA essaie d'accéder à la conversation de userB
curl -X GET "https://api.serenia.studio/conversations/${CONV_ID_USER_B}/messages" \
  -H "Authorization: Bearer ${JWT_USER_A}"
# Attendu : 403 Forbidden ou 404 Not Found (jamais 200 avec données de B)

# Test IDOR #2 : Suppression de la conversation d'un autre user
curl -X DELETE "https://api.serenia.studio/conversations/${CONV_ID_USER_B}" \
  -H "Authorization: Bearer ${JWT_USER_A}"
# Attendu : 403 ou 404

# Test IDOR #3 : Injection UUID aléatoire
curl -X GET "https://api.serenia.studio/conversations/$(uuidgen)/messages" \
  -H "Authorization: Bearer ${JWT_USER_A}"
# Attendu : 404 (pas d'erreur serveur 500)

# Test IDOR #4 : Paramètre userId dans le body (si endpoint existe)
curl -X POST "https://api.serenia.studio/conversations/add-message" \
  -H "Authorization: Bearer ${JWT_USER_A}" \
  -H "Content-Type: application/json" \
  -d '{"content":"test", "userId":"${USER_B_ID}"}'
# Attendu : userId ignoré, message créé pour userA uniquement
```

---

## Architecture

### Flux de sécurité des données

```
┌────────────────────────────────────────────────────────────────────┐
│                          CLIENT REQUEST                             │
│  POST /conversations/add-message                                    │
│  Headers: Authorization: Bearer eyJ...                              │
│  Body: { "content": "Hello" }                                       │
└────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────────┐
│                      JWT VALIDATION (Quarkus)                       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  1. Vérification signature RSA                               │  │
│  │  2. Vérification expiration                                  │  │
│  │  3. Extraction du subject (userId)                           │  │
│  └──────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────────┐
│                      RESOURCE LAYER                                 │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  UUID userId = UUID.fromString(jwt.getSubject());            │  │
│  │  // userId JAMAIS depuis request body ou path param          │  │
│  └──────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                                  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  Conversation conv = repository.findByIdAndUser(id, userId); │  │
│  │  // Toujours inclure userId dans les requêtes                │  │
│  └──────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌────────────────────────────────────────────────────────────────────┐
│                      REPOSITORY LAYER                               │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  SQL: SELECT * FROM conversation                             │  │
│  │       WHERE id = :convId AND user_id = :userId               │  │
│  │  // Clause userId OBLIGATOIRE                                │  │
│  └──────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────┘
```

### Matrice de contrôle d'accès

| Endpoint | Méthode | Auth Required | Admin Only | IDOR Protected |
|----------|---------|---------------|------------|----------------|
| `/auth/login` | POST | ❌ | ❌ | N/A |
| `/auth/register` | POST | ❌ | ❌ | N/A |
| `/conversations/add-message` | POST | ✅ | ❌ | ✅ (userId from JWT) |
| `/conversations/my-messages` | GET | ✅ | ❌ | ✅ (userId from JWT) |
| `/conversations/my-conversations` | DELETE | ✅ | ❌ | ✅ (userId from JWT) |
| `/profile` | GET | ✅ | ❌ | ✅ (email from JWT) |
| `/profile` | DELETE | ✅ | ❌ | ✅ (email from JWT) |
| `/admin/dashboard` | GET | ✅ | ✅ | N/A |
| `/admin/users` | GET | ✅ | ✅ | N/A |
| `/admin/users/{email}` | GET | ✅ | ✅ | N/A (admin sees all) |
| `/stripe/webhook` | POST | ❌ | ❌ | ✅ (signature Stripe) |

---

## Tests d'Acceptance

### TA-2.1 : Isolation des Conversations

| # | Précondition | Action | Résultat Attendu |
|---|--------------|--------|------------------|
| 1 | UserA et UserB ont des conversations | UserA GET `/conversations/my-messages` | Uniquement les messages de UserA |
| 2 | Idem | UserB GET `/conversations/my-messages` | Uniquement les messages de UserB |
| 3 | UserA connaît l'ID conv de UserB | UserA tente d'accéder via manipulation URL | 404 Not Found |

### TA-2.2 : Tentatives IDOR

| # | Attaque | Endpoint | Résultat Attendu |
|---|---------|----------|------------------|
| 1 | UUID conversation d'un autre user | GET `/conversations/{otherUserConvId}/messages` | 403 ou 404 |
| 2 | UUID aléatoire | GET `/conversations/{random-uuid}/messages` | 404 |
| 3 | userId dans body request | POST `/conversations/add-message` avec userId | userId ignoré, msg pour user JWT |
| 4 | Enumération séquentielle | GET `/conversations/1`, `/conversations/2`, ... | Tous 404 (pas d'int, UUID) |

### TA-2.3 : Contrôle RBAC Admin

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | User normal accède admin | GET `/admin/dashboard` avec JWT user | 403 Forbidden |
| 2 | Admin accède admin | GET `/admin/dashboard` avec JWT admin | 200 OK |
| 3 | User tente rôle dans JWT | JWT forgé avec role=ADMIN | 401 (signature invalide) |
| 4 | Admin sans role claim | JWT admin sans claim groups | 403 Forbidden |

### TA-2.4 : Suppression en Cascade (RGPD)

| # | Scénario | Action | Vérification |
|---|----------|--------|--------------|
| 1 | User supprime son compte | DELETE `/profile` | Toutes les tables user_id = X vides |
| 2 | Vérification BDD | Query directe PostgreSQL | `SELECT count(*) FROM messages WHERE user_id = X` = 0 |
| 3 | Vérification conversations | Idem | `SELECT count(*) FROM conversations WHERE user_id = X` = 0 |

**Script de vérification SQL :**
```sql
-- Après suppression du compte user X
SELECT 'users' as table_name, count(*) as remaining FROM users WHERE id = 'X'
UNION ALL
SELECT 'conversations', count(*) FROM conversations WHERE user_id = 'X'
UNION ALL
SELECT 'messages', count(*) FROM messages WHERE user_id = 'X';
-- Attendu : 0 pour toutes les lignes
```

---

## Vulnérabilités Potentielles à Corriger

### Si trouvées lors de l'audit :

#### V-2.1 : Endpoint sans vérification userId

**Symptôme :** Méthode service appelée sans `userId`

**Correction :**
```java
// AVANT (vulnérable)
@GET
@Path("/{convId}")
public Response getConversation(@PathParam("convId") UUID convId) {
    return service.getConversation(convId);  // ❌
}

// APRÈS (sécurisé)
@GET
@Path("/{convId}")
public Response getConversation(@PathParam("convId") UUID convId) {
    UUID userId = getAuthenticatedUserId();
    return service.getConversation(convId, userId);  // ✅
}
```

#### V-2.2 : Repository sans clause userId

**Correction :**
```java
// Supprimer ou rendre private
// public Optional<Conversation> findByConversationId(UUID conversationId)

// Toujours utiliser
public Optional<Conversation> findByIdAndUser(UUID conversationId, UUID userId)
```

---

## Critères de Complétion

- [ ] Tous les endpoints REST audités et documentés dans la matrice d'accès
- [ ] Aucune méthode repository publique sans paramètre `userId` (sauf lookups par email pour auth)
- [ ] Tests IDOR manuels (TA-2.2) passent à 100%
- [ ] Tests RBAC admin (TA-2.3) passent à 100%
- [ ] Cascade delete vérifiée en BDD (TA-2.4)
- [ ] Code review des services terminée avec checklist signée
- [ ] Aucun endpoint ne retourne de données d'un autre utilisateur
