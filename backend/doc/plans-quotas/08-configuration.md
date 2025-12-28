# Phase 8 : Configuration et Finalisation

**Durée estimée:** 0.5h

**Prérequis:** Phases 1 à 7 complétées

---

## 8.1 Principe de configuration

### Source unique de vérité : la base de données

**Toute la configuration des plans et quotas est stockée uniquement en base de données.**

- ❌ Pas de fichier `application.properties` pour les limites de plans
- ❌ Pas de variables d'environnement pour les quotas
- ✅ Les valeurs sont initialisées via la migration Liquibase
- ✅ Les modifications se font directement en base de données
- ✅ Les changements sont appliqués immédiatement (pas de redémarrage)

### Avantages

1. **Simplicité** : Une seule source de vérité
2. **Flexibilité** : Modification à chaud sans redéploiement
3. **Cohérence** : Pas de désynchronisation config/BDD

---

## 8.2 Calcul des tokens

### Stratégie MVP : strlen simple

Pour le MVP, le calcul des tokens est **simplifié au maximum** :

```
tokens_consommés = strlen(message_utilisateur) + strlen(réponse_assistant)
```

**Pourquoi cette approche ?**
- Simple à implémenter
- Pas de dépendance externe (tiktoken, etc.)
- Suffisant pour le MVP

**Évolution post-MVP :**
- Utiliser le champ `usage.total_tokens` retourné par l'API OpenAI
- Mise à jour du compteur APRÈS l'appel LLM avec la vraie valeur

### Implémentation simplifiée

Le `TokenEstimationService` devient très simple :

```java
package com.lofo.serenia.service.subscription;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service de calcul des tokens consommés.
 * 
 * MVP : Utilise strlen (longueur de chaîne) comme approximation.
 * Post-MVP : Utilisera les valeurs réelles retournées par l'API OpenAI.
 */
@ApplicationScoped
public class TokenCountingService {

    /**
     * Calcule le nombre de tokens pour un texte.
     * MVP : 1 caractère = 1 token (approximation grossière mais simple).
     *
     * @param content le texte
     * @return le nombre de "tokens" (= nombre de caractères)
     */
    public int countTokens(String content) {
        if (content == null) {
            return 0;
        }
        return content.length();
    }

    /**
     * Calcule le total de tokens pour un échange (entrée + sortie).
     *
     * @param userMessage le message de l'utilisateur
     * @param assistantResponse la réponse de l'assistant
     * @return le total de tokens consommés
     */
    public int countExchangeTokens(String userMessage, String assistantResponse) {
        return countTokens(userMessage) + countTokens(assistantResponse);
    }
}
```

---

## 8.3 Modification du flux de quota

### Changement important

Avec l'approche `strlen(entrée + sortie)`, on ne peut plus **réserver** les tokens AVANT l'appel LLM car on ne connaît pas la taille de la réponse.

**Nouveau flux :**

1. **AVANT l'appel LLM** : Vérifier uniquement la limite journalière de messages
2. **APRÈS l'appel LLM** : Calculer et enregistrer les tokens consommés (entrée + sortie)
3. **À la prochaine requête** : Vérifier que le quota mensuel n'est pas dépassé

### QuotaService adapté

```java
package com.lofo.serenia.service.subscription;

import com.lofo.serenia.exception.exceptions.QuotaExceededException;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@ApplicationScoped
@AllArgsConstructor
public class QuotaService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final TokenCountingService tokenCountingService;

    /**
     * Vérifie les quotas AVANT l'appel LLM.
     * Vérifie uniquement :
     * - La limite journalière de messages
     * - La limite mensuelle de tokens (consommation actuelle)
     * 
     * Note: La limite par message n'est plus vérifiée en amont car on utilise
     * strlen(entrée + sortie) et on ne connaît pas la sortie à ce stade.
     */
    @Transactional
    public void checkQuotaBeforeCall(UUID userId) {
        Subscription subscription = subscriptionRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> subscriptionService.createDefaultSubscription(userId));

        resetExpiredPeriods(subscription);
        Plan plan = subscription.getPlan();

        // Vérifier la limite mensuelle (tokens déjà consommés)
        if (subscription.getTokensUsedThisMonth() >= plan.getMonthlyTokenLimit()) {
            log.warn("User {} has exhausted monthly token limit: {}/{}", 
                    userId, subscription.getTokensUsedThisMonth(), plan.getMonthlyTokenLimit());
            throw QuotaExceededException.monthlyTokenLimit(
                    plan.getMonthlyTokenLimit(),
                    subscription.getTokensUsedThisMonth(),
                    0
            );
        }

        // Vérifier la limite journalière de messages
        if (subscription.getMessagesSentToday() >= plan.getDailyMessageLimit()) {
            log.warn("User {} reached daily message limit: {}/{}", 
                    userId, subscription.getMessagesSentToday(), plan.getDailyMessageLimit());
            throw QuotaExceededException.dailyMessageLimit(
                    plan.getDailyMessageLimit(),
                    subscription.getMessagesSentToday()
            );
        }
    }

    /**
     * Enregistre la consommation APRÈS l'appel LLM.
     * Calcule les tokens comme strlen(entrée) + strlen(sortie).
     *
     * @param userId l'ID de l'utilisateur
     * @param userMessage le message envoyé par l'utilisateur
     * @param assistantResponse la réponse de l'assistant
     */
    @Transactional
    public void recordUsage(UUID userId, String userMessage, String assistantResponse) {
        Subscription subscription = subscriptionRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("Subscription not found for user: " + userId));

        int tokensUsed = tokenCountingService.countExchangeTokens(userMessage, assistantResponse);

        subscription.setTokensUsedThisMonth(subscription.getTokensUsedThisMonth() + tokensUsed);
        subscription.setMessagesSentToday(subscription.getMessagesSentToday() + 1);
        
        subscriptionRepository.persist(subscription);

        log.debug("Recorded usage for user {}: {} tokens, {} messages today", 
                userId, tokensUsed, subscription.getMessagesSentToday());
    }

    // ... reste du code (resetExpiredPeriods, canSendMessage, etc.)
}
```

### ChatOrchestrator adapté

```java
@Transactional
public ProcessedMessageResult processUserMessage(UUID userId, String content) {
    // 1. Récupérer ou créer la conversation
    Conversation conv = conversationService.getOrCreateActiveConversation(userId);
    
    // 2. Vérifier les quotas AVANT l'appel (messages/jour, tokens/mois actuels)
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
```

---

## 8.4 Valeurs par défaut en base

Les valeurs sont initialisées dans la migration Liquibase (voir Phase 1).

Rappel des valeurs seed :

| Plan | Tokens/message | Tokens/mois | Messages/jour |
|------|----------------|-------------|---------------|
| FREE | 1000 | 10000 | 10 |
| PLUS | 4000 | 100000 | 50 |
| MAX | 8000 | 500000 | 200 |

**Pour modifier ces valeurs :**

```sql
-- Exemple : Augmenter la limite journalière du plan FREE
UPDATE plans 
SET daily_message_limit = 20, updated_at = NOW() 
WHERE name = 'FREE';

-- Les changements sont effectifs immédiatement
```

---

## 8.5 Pas de configuration externe

### Ce qu'on NE fait PAS

```properties
# ❌ PAS de config dans application.properties
# serenia.plans.free.daily-message-limit=10
# serenia.tokens.chars-per-token=3.5
```

```java
// ❌ PAS de classe QuotaConfig
// @ConfigMapping(prefix = "serenia.tokens")
// public interface QuotaConfig { ... }
```

```yaml
# ❌ PAS de variables d'environnement pour les plans
# SERENIA_PLANS_FREE_DAILY_MESSAGE_LIMIT: ${FREE_DAILY_LIMIT:-10}
```

### Ce qu'on FAIT

- ✅ Migration Liquibase avec valeurs initiales
- ✅ Lecture directe depuis la table `plans`
- ✅ Modification via SQL ou future interface admin

---

## 8.6 Tâches

- [ ] Supprimer `TokenEstimationService` et créer `TokenCountingService` (strlen simple)
- [ ] Adapter `QuotaService` : vérification avant, enregistrement après
- [ ] Adapter `ChatOrchestrator` pour le nouveau flux
- [ ] Mettre à jour les tests unitaires correspondants
- [ ] Vérifier que la migration Liquibase contient les bonnes valeurs seed

---

## 8.7 Checklist finale

### Validation complète

- [ ] La migration Liquibase s'exécute sans erreur
- [ ] Les 3 plans sont créés en base de données avec les bonnes valeurs
- [ ] L'inscription crée automatiquement une subscription FREE
- [ ] L'endpoint `/api/subscription/status` retourne le bon statut
- [ ] La limite journalière de messages est vérifiée AVANT l'appel LLM
- [ ] La limite mensuelle de tokens est vérifiée AVANT l'appel LLM
- [ ] Les tokens sont comptés APRÈS l'appel LLM (strlen entrée + sortie)
- [ ] Les compteurs sont incrémentés après chaque message
- [ ] Les périodes sont réinitialisées automatiquement
- [ ] Les erreurs de quota retournent un 429 avec les détails
- [ ] Tous les tests unitaires passent
- [ ] Tous les tests d'intégration passent

### Commandes de validation

```bash
# Compilation
./mvnw compile

# Tests unitaires
./mvnw test

# Tests d'intégration
./mvnw verify

# Lancement local
./mvnw quarkus:dev

# Vérification des plans en base
psql -h localhost -U serenia -d serenia -c "SELECT name, per_message_token_limit, monthly_token_limit, daily_message_limit FROM plans;"

# Vérification des subscriptions
psql -h localhost -U serenia -d serenia -c "SELECT s.id, u.email, p.name as plan, s.tokens_used_this_month, s.messages_sent_today FROM subscriptions s JOIN users u ON s.user_id = u.id JOIN plans p ON s.plan_id = p.id;"
```

---

## 8.8 Évolution post-MVP

### Utilisation des tokens réels OpenAI

Après le MVP, le comptage des tokens pourra utiliser la réponse de l'API OpenAI :

```java
// Dans ChatCompletionService ou un wrapper
public ChatCompletionResult generateReply(String systemPrompt, List<ChatMessage> history) {
    // Appel à l'API OpenAI
    ChatCompletion response = openAiClient.chat(...)
    
    // Récupérer les tokens réels
    Usage usage = response.getUsage();
    int totalTokens = usage.getTotalTokens(); // prompt_tokens + completion_tokens
    
    return new ChatCompletionResult(
        response.getChoices().get(0).getMessage().getContent(),
        totalTokens
    );
}

// Dans QuotaService.recordUsage()
public void recordUsage(UUID userId, int actualTokensUsed) {
    // Utiliser la vraie valeur au lieu de strlen
    subscription.setTokensUsedThisMonth(
        subscription.getTokensUsedThisMonth() + actualTokensUsed
    );
}
```

---

[← Phase précédente : Tests d'intégration](./07-tests-integration.md) | [Retour au README](./README.md)

