# Phase 4 : Intégration avec le code existant

**Durée estimée:** 2-3h

**Prérequis:** Phases 1, 2 et 3 complétées

---

## 4.1 Modification du ChatOrchestrator

### Fichier à modifier

`src/main/java/com/lofo/serenia/service/chat/ChatOrchestrator.java`

### Code actuel

```java
package com.lofo.serenia.service.chat;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.lofo.serenia.persistence.entity.conversation.Conversation;
import com.lofo.serenia.persistence.entity.conversation.Message;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

import java.util.UUID;

@AllArgsConstructor
@ApplicationScoped
public class ChatOrchestrator {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ChatCompletionService chatCompletionService;
    private final SereniaConfig sereniaConfig;

    @Transactional
    public ProcessedMessageResult processUserMessage(UUID userId, String content) {
        Conversation conv = conversationService.getOrCreateActiveConversation(userId);
        messageService.persistUserMessage(userId, conv.getId(), content);

        String assistantReply = chatCompletionService.generateReply(sereniaConfig.systemPrompt(),
                messageService.decryptConversationMessages(userId, conv.getId()));

        Message assistantMsg = messageService.persistAssistantMessage(userId, conv.getId(), assistantReply);
        ChatMessage chatMessage = new ChatMessage(assistantMsg.getRole(), assistantReply);

        return new ProcessedMessageResult(conv.getId(), chatMessage);
    }
}
```

### Code modifié

```java
package com.lofo.serenia.service.chat;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.persistence.entity.conversation.ChatMessage;
import com.lofo.serenia.persistence.entity.conversation.Conversation;
import com.lofo.serenia.persistence.entity.conversation.Message;
import com.lofo.serenia.service.subscription.QuotaService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@ApplicationScoped
public class ChatOrchestrator {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ChatCompletionService chatCompletionService;
    private final SereniaConfig sereniaConfig;
    private final QuotaService quotaService;

    @Transactional
    public ProcessedMessageResult processUserMessage(UUID userId, String content) {
        // 1. Récupérer ou créer la conversation
        Conversation conv = conversationService.getOrCreateActiveConversation(userId);
        
        // 2. Vérifier les quotas AVANT l'appel LLM (messages/jour, tokens/mois)
        // Lève QuotaExceededException si limite atteinte
        quotaService.checkQuotaBeforeCall(userId);
        
        // 3. Persister le message utilisateur
        messageService.persistUserMessage(userId, conv.getId(), content);

        // 4. Appeler le LLM
        List<ChatMessage> history = messageService.decryptConversationMessages(userId, conv.getId());
        String assistantReply = chatCompletionService.generateReply(
                sereniaConfig.systemPrompt(),
                history
        );

        // 5. Persister la réponse de l'assistant
        Message assistantMsg = messageService.persistAssistantMessage(userId, conv.getId(), assistantReply);
        
        // 6. Enregistrer la consommation APRÈS l'appel (strlen entrée + sortie)
        quotaService.recordUsage(userId, content, assistantReply);

        ChatMessage chatMessage = new ChatMessage(assistantMsg.getRole(), assistantReply);
        return new ProcessedMessageResult(conv.getId(), chatMessage);
    }
}
```

### Points clés de la modification

1. **Injection du QuotaService** : Un seul nouveau service à injecter
2. **Vérification AVANT** : `checkQuotaBeforeCall()` vérifie messages/jour et tokens/mois actuels
3. **Enregistrement APRÈS** : `recordUsage()` calcule strlen(entrée + sortie) et met à jour les compteurs
4. **Pas de rollback** : Pas besoin de libérer les quotas en cas d'erreur car on enregistre seulement après succès
5. **Logging** : Ajout de logs pour le debugging

---

## 4.2 Modification du RegistrationService

### Fichier à modifier

`src/main/java/com/lofo/serenia/service/user/RegistrationService.java`

### Modification à apporter

Après la création et la persistance de l'utilisateur, créer automatiquement une subscription avec le plan FREE.

```java
// Ajouter l'injection
private final SubscriptionService subscriptionService;

// Dans la méthode de création d'utilisateur, après userRepository.persist(user) :
public User registerUser(...) {
    // ... code existant de création de l'utilisateur ...
    
    userRepository.persist(user);
    
    // Créer automatiquement une subscription FREE pour le nouvel utilisateur
    subscriptionService.createDefaultSubscription(user.getId());
    
    // ... reste du code ...
}
```

### Exemple complet

```java
package com.lofo.serenia.service.user;

// ... imports existants ...
import com.lofo.serenia.service.subscription.SubscriptionService;

@Slf4j
@ApplicationScoped
@AllArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final MailService mailService;
    private final EncryptionService encryptionService;
    private final SubscriptionService subscriptionService; // NOUVEAU

    @Transactional
    public User registerUser(RegistrationRequestDTO request) {
        // ... validation et création de l'utilisateur ...
        
        User user = User.builder()
                .email(request.email())
                .password(encryptionService.hashPassword(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .accountActivated(false)
                .role(Role.USER)
                .build();

        userRepository.persist(user);
        
        // NOUVEAU : Créer une subscription FREE pour le nouvel utilisateur
        subscriptionService.createDefaultSubscription(user.getId());
        log.info("Created FREE subscription for new user {}", user.getId());

        // ... envoi email d'activation, etc. ...

        return user;
    }
}
```

---

## 4.3 Gestion de l'exception QuotaExceededException

### Option 1 : Handler global (recommandé)

**Fichier à créer:** `src/main/java/com/lofo/serenia/exception/handler/QuotaExceededHandler.java`

```java
package com.lofo.serenia.exception.handler;

import com.lofo.serenia.exception.exceptions.QuotaExceededException;
import com.lofo.serenia.rest.dto.out.QuotaErrorDTO;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler pour les exceptions de quota dépassé.
 * Retourne un status 429 avec les détails du quota.
 */
@Slf4j
@Provider
public class QuotaExceededHandler implements ExceptionMapper<QuotaExceededException> {

    @Override
    public Response toResponse(QuotaExceededException exception) {
        log.warn("Quota exceeded: {}", exception.getMessage());

        QuotaErrorDTO errorDTO = new QuotaErrorDTO(
                exception.getQuotaType().getCode(),
                exception.getLimit(),
                exception.getCurrent(),
                exception.getRequested(),
                exception.getMessage()
        );

        return Response.status(429)
                .entity(errorDTO)
                .build();
    }
}
```

### DTO QuotaErrorDTO

**Fichier à créer:** `src/main/java/com/lofo/serenia/rest/dto/out/QuotaErrorDTO.java`

```java
package com.lofo.serenia.rest.dto.out;

/**
 * DTO pour les erreurs de quota.
 */
public record QuotaErrorDTO(
        String quotaType,
        int limit,
        int current,
        int requested,
        String message
) {
}
```

### Option 2 : Géré par le handler existant SereniaException

Si `QuotaExceededException` étend `SereniaException`, elle sera automatiquement gérée par le handler existant.

---

## 4.4 Migration des utilisateurs existants

### Script de migration

Pour les utilisateurs existants sans subscription, un script doit être exécuté pour créer leurs subscriptions.

**Option 1 : Migration Liquibase (data migration)**

```yaml
# Dans 02-plans-subscriptions.yaml, ajouter à la fin :
  - changeSet:
      id: 02-migrate-existing-users
      author: [votre-nom]
      changes:
        - sql:
            sql: |
              INSERT INTO subscriptions (id, user_id, plan_id, tokens_used_this_month, 
                  messages_sent_today, monthly_period_start, daily_period_start, 
                  created_at, updated_at)
              SELECT 
                  gen_random_uuid(),
                  u.id,
                  (SELECT id FROM plans WHERE name = 'FREE'),
                  0,
                  0,
                  NOW(),
                  NOW(),
                  NOW(),
                  NOW()
              FROM users u
              WHERE NOT EXISTS (
                  SELECT 1 FROM subscriptions s WHERE s.user_id = u.id
              )
```

**Option 2 : Création à la volée (déjà implémentée)**

Le `QuotaService.checkAndReserveQuota()` appelle `subscriptionService.createDefaultSubscription()` si aucune subscription n'existe. Cette approche est plus simple mais crée la subscription au premier message.

---

## 4.5 Tâches

- [ ] Modifier `ChatOrchestrator.java` pour intégrer les vérifications de quota
- [ ] Modifier `RegistrationService.java` pour créer la subscription à l'inscription
- [ ] Créer `QuotaExceededHandler.java` (optionnel si handler générique existe)
- [ ] Créer `QuotaErrorDTO.java`
- [ ] Ajouter la migration des utilisateurs existants
- [ ] Tester manuellement le flux complet

---

## 4.6 Tests de validation manuelle

```bash
# 1. Créer un utilisateur
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!","firstName":"Test","lastName":"User"}'

# 2. Se connecter et obtenir un token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test123!"}' | jq -r '.token')

# 3. Vérifier le statut de subscription
curl http://localhost:8080/api/subscription/status \
  -H "Authorization: Bearer $TOKEN"

# 4. Envoyer un message
curl -X POST http://localhost:8080/api/conversations/add-message \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"Hello, how are you?"}'

# 5. Vérifier que les compteurs ont été incrémentés
curl http://localhost:8080/api/subscription/status \
  -H "Authorization: Bearer $TOKEN"
```

---

[← Phase précédente : Services](./03-services.md) | [Retour au README](./README.md) | [Phase suivante : API REST →](./05-api-rest.md)

