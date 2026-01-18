# Step 5 : Évaluation de la Conformité RGPD et Suppression des Données

## Contexte

L'application Serenia collecte et traite des données personnelles sensibles :
- Conversations privées entre utilisateurs et IA (potentiellement données de santé mentale)
- Informations de compte (email, nom, prénom)
- Données de paiement (via Stripe, mais métadonnées stockées)
- Données de navigation et d'usage

En tant qu'application traitant des données de résidents européens, Serenia doit être conforme au RGPD (Règlement Général sur la Protection des Données).

### Fichiers concernés

| Fichier | Rôle RGPD |
|---------|-----------|
| `ProfileResource.java` | Endpoint DELETE pour droit à l'effacement |
| `AccountManagementService.java` | Logique de suppression cascade |
| `ConversationRepository.java` | Suppression des conversations |
| `MessageRepository.java` | Suppression des messages |
| `UserRepository.java` | Suppression du compte utilisateur |

### État actuel détecté

- ✅ Endpoint `DELETE /profile` existe pour suppression de compte
- ✅ Méthodes `deleteByUserId()` dans les repositories
- ⚠️ Cascade de suppression à vérifier
- ⚠️ Rétention des logs à auditer
- ✅ Messages chiffrés (protection des données at-rest)

---

## Objectif

1. **Valider le droit à l'effacement (Art. 17 RGPD)** : Suppression complète de toutes les données utilisateur
2. **Vérifier la minimisation des données (Art. 5)** : Collecter uniquement ce qui est nécessaire
3. **Auditer la rétention des données** : Durées de conservation définies et respectées
4. **Contrôler les transferts de données** : OpenAI (US) → conformité avec décisions d'adéquation
5. **Documenter les traitements** : Registre des traitements à jour

---

## Méthode

### 5.1 Audit du Droit à l'Effacement

#### AccountManagementService.java - Analyse

```java
@Transactional
public void deleteAccountAndAssociatedData(String email) {
    log.info("Deleting account for email=%s", email);
    User user = userFinder.findByEmailOrThrow(email);
    UUID userId = user.getId();

    // Suppression en cascade manuelle
    messageRepository.deleteByUserId(userId);        // ✅ Messages
    conversationRepository.deleteByUserId(userId);   // ✅ Conversations
    long deletedUsers = userRepository.deleteById(userId);  // ✅ User

    if (deletedUsers != 1) {
        log.error("Unexpected delete result for user %s: %d rows", email, deletedUsers);
        throw new WebApplicationException(ERROR_ACCOUNT_DELETION_FAILED, Response.Status.INTERNAL_SERVER_ERROR);
    }

    log.info("User %s and related data deleted", email);
}
```

#### Checklist de suppression complète

| Donnée | Table/Collection | Méthode de suppression | Vérifié |
|--------|-----------------|----------------------|---------|
| Compte utilisateur | `users` | `userRepository.deleteById()` | ⬜ |
| Conversations | `conversations` | `conversationRepository.deleteByUserId()` | ⬜ |
| Messages | `messages` | `messageRepository.deleteByUserId()` | ⬜ |
| Tokens/quotas | `user_quotas` | ⬜ À vérifier | ⬜ |
| Abonnements Stripe | `subscriptions` | ⬜ À vérifier | ⬜ |
| Logs applicatifs | Log files | ⬜ Politique de rotation | ⬜ |
| Données Stripe | Stripe Dashboard | Demande séparée ? | ⬜ |
| Données OpenAI | OpenAI API | Pas de stockage côté OpenAI (par défaut) | ⬜ |

#### Recherche des tables liées à l'utilisateur

```bash
# Lister toutes les entités avec userId ou user_id
grep -rn "user_id\|userId\|@ManyToOne.*User" backend/src/main/java/com/lofo/serenia/persistence/entity/

# Vérifier les foreign keys en BDD
docker exec serenia-db psql -U serenia -d serenia -c "
SELECT 
    tc.table_name, 
    kcu.column_name,
    ccu.table_name AS foreign_table_name
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY' 
    AND ccu.table_name = 'users';
"
```

#### Vérification des données manquantes dans la cascade

```java
// Ajouter dans AccountManagementService si tables supplémentaires trouvées
@Transactional
public void deleteAccountAndAssociatedData(String email) {
    User user = userFinder.findByEmailOrThrow(email);
    UUID userId = user.getId();

    // Ordre important : enfants d'abord, puis parents
    messageRepository.deleteByUserId(userId);
    conversationRepository.deleteByUserId(userId);
    
    // ⚠️ Ajouter si ces tables existent
    // quotaRepository.deleteByUserId(userId);
    // subscriptionRepository.deleteByUserId(userId);
    // auditLogRepository.anonymizeByUserId(userId);  // ou supprimer
    
    userRepository.deleteById(userId);
}
```

### 5.2 Audit de la Minimisation des Données

#### Données collectées vs nécessaires

| Donnée | Collectée | Nécessaire | Justification |
|--------|-----------|------------|---------------|
| Email | ✅ | ✅ | Authentification, communication |
| Prénom | ✅ | ⚠️ | Personnalisation (optionnel possible) |
| Nom | ✅ | ⚠️ | Personnalisation (optionnel possible) |
| Mot de passe (hash) | ✅ | ✅ | Authentification |
| Messages chat | ✅ | ✅ | Fonctionnalité principale |
| IP Address | ⚠️ | ⚠️ | Logs, sécurité (à limiter) |
| User Agent | ⚠️ | ❌ | Non nécessaire |

#### Vérifier les logs applicatifs

```bash
# Rechercher les données personnelles dans les logs
grep -rn "log\.\|LOG\." backend/src/main/java/ | grep -i "email\|name\|password\|content"

# Exemples problématiques
log.info("User login: email={}", dto.email());           // ⚠️ Email en clair
log.debug("Message content: {}", message.getContent());  // ❌ Données sensibles
```

**Correction des logs :**
```java
// ❌ AVANT
log.info("User login attempted for email=%s", dto.email());

// ✅ APRÈS - Masquer partiellement ou hasher
log.info("User login attempted for email={}", maskEmail(dto.email()));
// ou
log.info("User login attempted for userId={}", user.getId());

private String maskEmail(String email) {
    if (email == null || !email.contains("@")) return "***";
    String[] parts = email.split("@");
    return parts[0].charAt(0) + "***@" + parts[1];
    // "john.doe@email.com" → "j***@email.com"
}
```

### 5.3 Audit de la Rétention des Données

#### Politique de rétention recommandée

| Type de donnée | Durée de rétention | Action à expiration |
|----------------|-------------------|---------------------|
| Compte actif | Illimitée (tant qu'actif) | N/A |
| Compte inactif | 2 ans | Notification puis suppression |
| Messages chat | Durée du compte | Suppression avec compte |
| Logs applicatifs | 90 jours | Rotation/suppression |
| Logs d'audit sécurité | 1 an | Anonymisation puis suppression |
| Données de paiement Stripe | Géré par Stripe | Cf. politique Stripe |

#### Implémentation de la purge automatique

```java
@ApplicationScoped
public class DataRetentionService {
    
    @ConfigProperty(name = "serenia.retention.inactive-account-days", defaultValue = "730")
    int inactiveAccountDays;
    
    @Scheduled(cron = "0 0 2 * * ?")  // Tous les jours à 2h
    @Transactional
    public void purgeInactiveAccounts() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(inactiveAccountDays);
        
        List<User> inactiveUsers = userRepository.findInactiveSince(cutoff);
        
        for (User user : inactiveUsers) {
            // Envoyer notification avant suppression (30 jours avant)
            if (shouldNotify(user, cutoff)) {
                notificationService.sendDeletionWarning(user);
            }
            
            // Supprimer si délai de grâce passé
            if (shouldDelete(user, cutoff)) {
                accountManagementService.deleteAccountAndAssociatedData(user.getEmail());
                log.info("Purged inactive account: userId={}", user.getId());
            }
        }
    }
}
```

### 5.4 Audit des Transferts de Données (OpenAI)

#### Contexte juridique

OpenAI est une entreprise américaine. Les transferts de données vers les US nécessitent :
- Data Privacy Framework (DPF) - OpenAI est certifié
- Clauses Contractuelles Types (CCT)
- Mesures techniques supplémentaires

#### Vérifications OpenAI

| Critère | Vérification | Statut |
|---------|--------------|--------|
| Certification DPF | Vérifier sur dataprivacyframework.gov | ⬜ |
| Data Processing Agreement | Signé avec OpenAI | ⬜ |
| Opt-out training | API data not used for training (par défaut depuis mars 2023) | ⬜ |
| Rétention OpenAI | 30 jours max (par défaut) | ⬜ |

**Vérification pratique :**
```bash
# Vérifier les headers de requête vers OpenAI
# Aucune donnée PII ne devrait être dans les headers ou metadata

# Vérifier la politique de l'organisation OpenAI
# Dashboard OpenAI > Settings > Data controls
# "API data is not used to train our models" doit être activé
```

#### Documentation à produire

**Fiche de traitement pour le registre RGPD :**
```
Traitement: Génération de réponses IA
Finalité: Fournir un assistant conversationnel
Base légale: Exécution du contrat (Art. 6.1.b)
Catégories de données: Messages texte (potentiellement sensibles)
Destinataires: OpenAI (sous-traitant, US)
Transfert hors UE: Oui - US (DPF + CCT)
Durée de conservation: 30 jours chez OpenAI, durée du compte chez Serenia
Mesures de sécurité: Chiffrement AES-256-GCM, TLS 1.3, accès restreint
```

### 5.5 Vérification du Chiffrement At-Rest

#### Messages en base de données

```sql
-- Vérifier que les messages sont bien chiffrés
SELECT 
    id,
    LENGTH(encrypted_content) as content_length,
    ENCODE(SUBSTRING(encrypted_content, 1, 10), 'hex') as content_preview
FROM messages 
LIMIT 5;

-- Le content_preview doit commencer par "01" (version byte)
-- et être illisible (pas de texte ASCII)
```

#### Vérification de non-stockage en clair

```bash
# Rechercher du texte en clair dans les colonnes sensibles
docker exec serenia-db psql -U serenia -d serenia -c "
SELECT id, encrypted_content::text 
FROM messages 
WHERE encrypted_content::text LIKE '%Hello%' 
   OR encrypted_content::text LIKE '%Bonjour%'
LIMIT 5;
"
# Attendu: 0 résultats (données chiffrées = pas de texte lisible)
```

---

## Architecture

### Flux de suppression RGPD

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     GDPR DELETION FLOW (Art. 17)                         │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────┐
│    User      │
│  Request     │
│  DELETE      │
└──────┬───────┘
       │
       │  DELETE /profile
       │  Authorization: Bearer <jwt>
       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         ProfileResource                                  │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │  1. Extract email from JWT (SecurityIdentity)                     │  │
│  │  2. Call AccountManagementService.deleteAccountAndAssociatedData()│  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    AccountManagementService                              │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │  @Transactional (atomique - tout ou rien)                         │  │
│  └───────────────────────────────────────────────────────────────────┘  │
│                                    │                                     │
│          ┌─────────────────────────┼─────────────────────────┐          │
│          │                         │                         │          │
│          ▼                         ▼                         ▼          │
│  ┌───────────────┐      ┌──────────────────┐      ┌─────────────────┐  │
│  │   Messages    │      │  Conversations   │      │   User Account  │  │
│  │   Repository  │      │    Repository    │      │    Repository   │  │
│  │               │      │                  │      │                 │  │
│  │ deleteByUser  │      │  deleteByUser    │      │   deleteById    │  │
│  │    Id()       │      │     Id()         │      │      ()         │  │
│  └───────┬───────┘      └────────┬─────────┘      └────────┬────────┘  │
│          │                       │                         │           │
│          ▼                       ▼                         ▼           │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                       PostgreSQL                                │   │
│  │   DELETE FROM messages WHERE user_id = ?                        │   │
│  │   DELETE FROM conversations WHERE user_id = ?                   │   │
│  │   DELETE FROM users WHERE id = ?                                │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Response: 204 No Content                         │
│                     (Account successfully deleted)                       │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                    POST-DELETION CONSIDERATIONS                          │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │  ⚠️ External Systems (manual or async process):                   │  │
│  │  - Stripe: Cancel subscription via API                            │  │
│  │  - OpenAI: No action (data retention 30 days auto)               │  │
│  │  - Logs: Will be purged via rotation (90 days)                   │  │
│  │  - Backups: Excluded from next backup or marked for deletion     │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### Cartographie des données personnelles

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    PERSONAL DATA MAP                                     │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  TABLE: users                                                            │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │ Column          │ PII │ Sensitive │ Encrypted │ Retention          ││
│  ├─────────────────┼─────┼───────────┼───────────┼────────────────────┤│
│  │ id (UUID)       │ ❌  │ ❌        │ ❌        │ Account lifetime   ││
│  │ email           │ ✅  │ ❌        │ ❌        │ Account lifetime   ││
│  │ first_name      │ ✅  │ ❌        │ ❌        │ Account lifetime   ││
│  │ last_name       │ ✅  │ ❌        │ ❌        │ Account lifetime   ││
│  │ password_hash   │ ❌  │ ❌        │ ✅ (bcrypt)│ Account lifetime   ││
│  │ created_at      │ ❌  │ ❌        │ ❌        │ Account lifetime   ││
│  │ last_login      │ ⚠️  │ ❌        │ ❌        │ Account lifetime   ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  TABLE: messages                                                         │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │ Column            │ PII │ Sensitive │ Encrypted │ Retention        ││
│  ├───────────────────┼─────┼───────────┼───────────┼──────────────────┤│
│  │ id (UUID)         │ ❌  │ ❌        │ ❌        │ Account lifetime ││
│  │ conversation_id   │ ❌  │ ❌        │ ❌        │ Account lifetime ││
│  │ user_id           │ ❌  │ ❌        │ ❌        │ Account lifetime ││
│  │ encrypted_content │ ✅  │ ✅ *      │ ✅ AES-256│ Account lifetime ││
│  │ role              │ ❌  │ ❌        │ ❌        │ Account lifetime ││
│  │ created_at        │ ❌  │ ❌        │ ❌        │ Account lifetime ││
│  └─────────────────────────────────────────────────────────────────────┘│
│  * Potentiellement données de santé mentale (catégorie spéciale Art.9)  │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  EXTERNAL: OpenAI                                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │ Data Type         │ Transferred │ Retention  │ Training Use        ││
│  ├───────────────────┼─────────────┼────────────┼─────────────────────┤│
│  │ Message content   │ ✅          │ 30 days    │ ❌ (API default)    ││
│  │ User identifier   │ ❌ *        │ N/A        │ N/A                 ││
│  └─────────────────────────────────────────────────────────────────────┘│
│  * Vérifier qu'aucun user_id/email n'est envoyé dans les messages       │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  EXTERNAL: Stripe                                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │ Data Type         │ Transferred │ Retention     │ Controller       ││
│  ├───────────────────┼─────────────┼───────────────┼──────────────────┤│
│  │ Email             │ ✅          │ Stripe policy │ Stripe           ││
│  │ Payment method    │ ✅          │ Stripe policy │ Stripe           ││
│  │ Transaction hist. │ ✅          │ Legal (7 ans) │ Stripe           ││
│  └─────────────────────────────────────────────────────────────────────┘│
│  Note: Stripe est responsable de traitement indépendant pour paiements  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Tests d'Acceptance

### TA-5.1 : Suppression Complète du Compte (Droit à l'Effacement)

| # | Précondition | Action | Vérification |
|---|--------------|--------|--------------|
| 1 | User avec messages | `DELETE /profile` | Response 204 |
| 2 | Post-suppression | Query `users` table | 0 rows for user_id |
| 3 | Post-suppression | Query `conversations` table | 0 rows for user_id |
| 4 | Post-suppression | Query `messages` table | 0 rows for user_id |
| 5 | Post-suppression | Query `user_quotas` table (si existe) | 0 rows for user_id |
| 6 | Login après suppression | `POST /auth/login` | 401 Unauthorized |

**Script de test complet :**
```bash
#!/bin/bash
USER_EMAIL="test-gdpr@test.com"

# 1. Créer un utilisateur test avec des données
# (via registration API ou directement en BDD pour le test)

# 2. Créer des messages
curl -X POST "https://api.serenia.studio/conversations/add-message" \
  -H "Authorization: Bearer $JWT" \
  -d '{"content":"Test message 1"}'

# 3. Récupérer le user_id
USER_ID=$(docker exec serenia-db psql -U serenia -d serenia -t -c \
  "SELECT id FROM users WHERE email = '$USER_EMAIL';" | tr -d ' ')

# 4. Compter les données avant suppression
echo "Before deletion:"
docker exec serenia-db psql -U serenia -d serenia -c "
SELECT 'users' as table_name, COUNT(*) FROM users WHERE id = '$USER_ID'
UNION ALL SELECT 'conversations', COUNT(*) FROM conversations WHERE user_id = '$USER_ID'
UNION ALL SELECT 'messages', COUNT(*) FROM messages WHERE user_id = '$USER_ID';"

# 5. Supprimer le compte
curl -X DELETE "https://api.serenia.studio/profile" \
  -H "Authorization: Bearer $JWT"

# 6. Vérifier la suppression
echo "After deletion:"
docker exec serenia-db psql -U serenia -d serenia -c "
SELECT 'users' as table_name, COUNT(*) FROM users WHERE id = '$USER_ID'
UNION ALL SELECT 'conversations', COUNT(*) FROM conversations WHERE user_id = '$USER_ID'
UNION ALL SELECT 'messages', COUNT(*) FROM messages WHERE user_id = '$USER_ID';"
# Attendu: 0 pour toutes les tables
```

### TA-5.2 : Atomicité de la Suppression

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | Échec partiel simulé | Couper connexion BDD pendant suppression | Rollback complet, aucune donnée supprimée |
| 2 | Contrainte FK violée | Ajouter FK sans ON DELETE CASCADE | Erreur 500, rollback |
| 3 | Succès | Suppression normale | Toutes les données supprimées atomiquement |

### TA-5.3 : Pas de Données Personnelles dans les Logs

| # | Vérification | Commande | Résultat Attendu |
|---|--------------|----------|------------------|
| 1 | Email masqué | `grep -i "email=" logs/app.log` | Format masqué (`j***@domain.com`) |
| 2 | Pas de mots de passe | `grep -i "password" logs/app.log` | 0 résultats ou "password=***" |
| 3 | Pas de contenu messages | `grep "content=" logs/app.log` | 0 résultats |
| 4 | IP anonymisée | `grep -E "\d+\.\d+\.\d+\.\d+" logs/app.log` | IPs anonymisées ou absentes |

### TA-5.4 : Chiffrement At-Rest Vérifié

| # | Scénario | Vérification | Résultat Attendu |
|---|----------|--------------|------------------|
| 1 | Message créé | Query BDD pour `encrypted_content` | Bytes illisibles, commence par 0x01 |
| 2 | Pas de plaintext | `SELECT * FROM messages WHERE encrypted_content::text LIKE '%Hello%'` | 0 résultats |
| 3 | Clé différente par user | Comparer ciphertexts de même message pour 2 users | Différents |

### TA-5.5 : Export des Données (Droit à la Portabilité - Art. 20)

| # | Scénario | Action | Résultat Attendu |
|---|----------|--------|------------------|
| 1 | Export demandé | `GET /profile/export` (si implémenté) | JSON/CSV avec toutes les données user |
| 2 | Format machine-readable | Vérifier format export | JSON structuré |
| 3 | Données complètes | Comparer export vs BDD | Toutes les données présentes |

**Note :** Si l'endpoint `/profile/export` n'existe pas, c'est une fonctionnalité à implémenter.

---

## Vulnérabilités et Améliorations Identifiées

### V-5.1 : Pas de table d'audit RGPD (RECOMMANDÉ)

**Risque :** Difficulté à prouver la conformité RGPD en cas de contrôle.

**Correction :**
```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID,
    action VARCHAR(50) NOT NULL,  -- LOGIN, DELETE_ACCOUNT, DATA_EXPORT, etc.
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    ip_address VARCHAR(45),  -- Anonymisée après 90 jours
    user_agent TEXT,
    details JSONB
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
```

### V-5.2 : Pas d'endpoint d'export des données (Art. 20)

**Correction recommandée :**
```java
@GET
@Path("/export")
@Produces(MediaType.APPLICATION_JSON)
public Response exportUserData() {
    String email = securityIdentity.getPrincipal().getName();
    UserDataExportDTO export = accountManagementService.exportUserData(email);
    return Response.ok(export)
        .header("Content-Disposition", "attachment; filename=my-data.json")
        .build();
}
```

### V-5.3 : Logs contenant potentiellement des emails en clair

**Correction :** Implémenter `maskEmail()` dans tous les logs.

### V-5.4 : Pas de politique de rétention automatisée

**Correction :** Implémenter `DataRetentionService` avec scheduled job.

---

## Critères de Complétion

- [ ] Suppression complète vérifiée (TA-5.1 passe)
- [ ] Atomicité de la suppression vérifiée (@Transactional)
- [ ] Toutes les tables avec user_id ont une méthode `deleteByUserId()`
- [ ] Logs ne contiennent pas de données personnelles en clair
- [ ] Messages chiffrés vérifiés en BDD
- [ ] Documentation RGPD produite (registre des traitements)
- [ ] Endpoint `/profile/export` implémenté (ou backlog priorisé)
- [ ] Politique de rétention documentée
- [ ] Annulation Stripe lors de suppression compte
- [ ] Tests TA-5.1 à TA-5.5 passent à 100%
