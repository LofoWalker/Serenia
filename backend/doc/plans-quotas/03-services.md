# Phase 3 : Couche Service

**Durée estimée:** 4-5h

**Prérequis:** Phases 1 et 2 complétées

---

## 3.1 Exception et Enum pour les quotas

### 3.1.1 Enum QuotaType

**Fichier:** `src/main/java/com/lofo/serenia/exception/exceptions/QuotaType.java`

```java
package com.lofo.serenia.exception.exceptions;

/**
 * Types de quotas pouvant être dépassés.
 */
public enum QuotaType {
    /**
     * Limite de tokens par message individuel.
     */
    MESSAGE_TOKEN_LIMIT("message_token_limit", "Message exceeds token limit"),

    /**
     * Limite mensuelle de tokens.
     */
    MONTHLY_TOKEN_LIMIT("monthly_token_limit", "Monthly token limit exceeded"),

    /**
     * Limite journalière de messages.
     */
    DAILY_MESSAGE_LIMIT("daily_message_limit", "Daily message limit reached");

    private final String code;
    private final String defaultMessage;

    QuotaType(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
```

---

### 3.1.2 QuotaExceededException

**Fichier:** `src/main/java/com/lofo/serenia/exception/exceptions/QuotaExceededException.java`

```java
package com.lofo.serenia.exception.exceptions;

import lombok.Getter;

/**
 * Exception levée lorsqu'un quota d'utilisation est dépassé.
 * Retourne un status HTTP 429 (Too Many Requests).
 */
@Getter
public class QuotaExceededException extends SereniaException {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private final QuotaType quotaType;
    private final int limit;
    private final int current;
    private final int requested;

    public QuotaExceededException(QuotaType quotaType, int limit, int current, int requested) {
        super(
                buildMessage(quotaType, limit, current, requested),
                HTTP_TOO_MANY_REQUESTS,
                "QUOTA_EXCEEDED_" + quotaType.getCode().toUpperCase()
        );
        this.quotaType = quotaType;
        this.limit = limit;
        this.current = current;
        this.requested = requested;
    }

    private static String buildMessage(QuotaType type, int limit, int current, int requested) {
        return switch (type) {
            case MESSAGE_TOKEN_LIMIT -> 
                String.format("Message too long: %d tokens requested, limit is %d tokens per message", 
                        requested, limit);
            case MONTHLY_TOKEN_LIMIT -> 
                String.format("Monthly token limit exceeded: %d/%d tokens used, %d requested", 
                        current, limit, requested);
            case DAILY_MESSAGE_LIMIT -> 
                String.format("Daily message limit reached: %d/%d messages sent today", 
                        current, limit);
        };
    }

    /**
     * Crée une exception pour dépassement de limite de tokens par message.
     */
    public static QuotaExceededException messageTokenLimit(int limit, int requested) {
        return new QuotaExceededException(QuotaType.MESSAGE_TOKEN_LIMIT, limit, 0, requested);
    }

    /**
     * Crée une exception pour dépassement de limite mensuelle.
     */
    public static QuotaExceededException monthlyTokenLimit(int limit, int current, int requested) {
        return new QuotaExceededException(QuotaType.MONTHLY_TOKEN_LIMIT, limit, current, requested);
    }

    /**
     * Crée une exception pour dépassement de limite journalière.
     */
    public static QuotaExceededException dailyMessageLimit(int limit, int current) {
        return new QuotaExceededException(QuotaType.DAILY_MESSAGE_LIMIT, limit, current, 1);
    }
}
```

---

## 3.2 TokenCountingService

### Fichier à créer

`src/main/java/com/lofo/serenia/service/subscription/TokenCountingService.java`

### Implémentation

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

## 3.3 QuotaService

### Fichier à créer

`src/main/java/com/lofo/serenia/service/subscription/QuotaService.java`

### Implémentation

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

/**
 * Service de gestion des quotas d'utilisation.
 * 
 * Flux MVP :
 * 1. AVANT l'appel LLM : vérifier messages/jour et tokens/mois actuels
 * 2. APRÈS l'appel LLM : enregistrer la consommation (strlen entrée + sortie)
 */
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
     * - La limite mensuelle de tokens (consommation actuelle)
     * - La limite journalière de messages
     *
     * @param userId l'ID de l'utilisateur
     * @throws QuotaExceededException si un quota est dépassé
     */
    @Transactional
    public void checkQuotaBeforeCall(UUID userId) {
        // 1. Récupérer la subscription avec lock pessimiste
        Subscription subscription = subscriptionRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> subscriptionService.createDefaultSubscription(userId));

        // 2. Vérifier et réinitialiser les périodes expirées
        resetExpiredPeriods(subscription);

        // 3. Récupérer le plan pour les limites
        Plan plan = subscription.getPlan();

        // 4. Vérifier la limite mensuelle de tokens (consommation actuelle)
        if (subscription.getTokensUsedThisMonth() >= plan.getMonthlyTokenLimit()) {
            log.warn("User {} has exhausted monthly token limit: {}/{}", 
                    userId, subscription.getTokensUsedThisMonth(), plan.getMonthlyTokenLimit());
            throw QuotaExceededException.monthlyTokenLimit(
                    plan.getMonthlyTokenLimit(),
                    subscription.getTokensUsedThisMonth(),
                    0
            );
        }

        // 5. Vérifier la limite journalière de messages
        if (subscription.getMessagesSentToday() >= plan.getDailyMessageLimit()) {
            log.warn("User {} reached daily message limit: {}/{}", 
                    userId, subscription.getMessagesSentToday(), plan.getDailyMessageLimit());
            throw QuotaExceededException.dailyMessageLimit(
                    plan.getDailyMessageLimit(),
                    subscription.getMessagesSentToday()
            );
        }

        log.debug("Quota check passed for user {}: {}/{} tokens, {}/{} messages", 
                userId, 
                subscription.getTokensUsedThisMonth(), plan.getMonthlyTokenLimit(),
                subscription.getMessagesSentToday(), plan.getDailyMessageLimit());
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

        log.debug("Recorded usage for user {}: {} tokens (total: {}), {} messages today", 
                userId, tokensUsed, subscription.getTokensUsedThisMonth(), 
                subscription.getMessagesSentToday());
    }

    /**
     * Vérifie si l'utilisateur peut envoyer un message sans réserver.
     * Utile pour l'affichage côté frontend.
     *
     * @param userId l'ID de l'utilisateur
     * @param estimatedTokens le nombre de tokens estimés
     * @return true si le message peut être envoyé
     */
    /**
     * Vérifie si l'utilisateur peut envoyer un message (sans modifier les compteurs).
     * Utile pour l'affichage côté frontend.
     *
     * @param userId l'ID de l'utilisateur
     * @return true si le message peut être envoyé
     */
    public boolean canSendMessage(UUID userId) {
        try {
            Subscription subscription = subscriptionRepository.findByUserId(userId)
                    .orElse(null);
            
            if (subscription == null) {
                return true; // Sera créé avec plan FREE
            }

            Plan plan = subscription.getPlan();

            // Vérifier la limite mensuelle de tokens
            if (subscription.getTokensUsedThisMonth() >= plan.getMonthlyTokenLimit()) {
                return false;
            }

            // Vérifier la limite journalière de messages
            if (subscription.getMessagesSentToday() >= plan.getDailyMessageLimit()) {
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Error checking quota for user {}", userId, e);
            return false;
        }
    }

    /**
     * Réinitialise les périodes expirées pour une subscription.
     *
     * @param subscription la subscription à vérifier/réinitialiser
     */
    private void resetExpiredPeriods(Subscription subscription) {
        boolean updated = false;

        if (subscription.isMonthlyPeriodExpired()) {
            log.info("Resetting monthly period for subscription {}", subscription.getId());
            subscription.resetMonthlyPeriod();
            updated = true;
        }

        if (subscription.isDailyPeriodExpired()) {
            log.info("Resetting daily period for subscription {}", subscription.getId());
            subscription.resetDailyPeriod();
            updated = true;
        }

        if (updated) {
            subscriptionRepository.persist(subscription);
        }
    }
}
```

---

## 3.4 SubscriptionService

### Fichier à créer

`src/main/java/com/lofo/serenia/service/subscription/SubscriptionService.java`

### Implémentation

```java
package com.lofo.serenia.service.subscription;

import com.lofo.serenia.exception.exceptions.SereniaException;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.rest.dto.out.SubscriptionStatusDTO;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service de gestion des subscriptions utilisateurs.
 */
@Slf4j
@ApplicationScoped
@AllArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;

    /**
     * Crée une subscription avec le plan FREE pour un nouvel utilisateur.
     *
     * @param userId l'ID de l'utilisateur
     * @return la subscription créée
     */
    @Transactional
    public Subscription createDefaultSubscription(UUID userId) {
        return createSubscription(userId, PlanType.FREE);
    }

    /**
     * Crée une subscription pour un utilisateur avec un plan spécifique.
     *
     * @param userId l'ID de l'utilisateur
     * @param planType le type de plan
     * @return la subscription créée
     */
    @Transactional
    public Subscription createSubscription(UUID userId, PlanType planType) {
        // Vérifier que l'utilisateur n'a pas déjà une subscription
        if (subscriptionRepository.existsByUserId(userId)) {
            throw SereniaException.conflict("User already has a subscription");
        }

        User user = userRepository.findById(userId);
        if (user == null) {
            throw SereniaException.notFound("User not found: " + userId);
        }

        Plan plan = planRepository.findByName(planType)
                .orElseThrow(() -> SereniaException.notFound("Plan not found: " + planType));

        LocalDateTime now = LocalDateTime.now();

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .tokensUsedThisMonth(0)
                .messagesSentToday(0)
                .monthlyPeriodStart(now)
                .dailyPeriodStart(now)
                .build();

        subscriptionRepository.persist(subscription);
        log.info("Created subscription for user {} with plan {}", userId, planType);

        return subscription;
    }

    /**
     * Récupère ou crée la subscription d'un utilisateur.
     *
     * @param userId l'ID de l'utilisateur
     * @return la subscription existante ou nouvellement créée
     */
    @Transactional
    public Subscription getOrCreateSubscription(UUID userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSubscription(userId));
    }

    /**
     * Change le plan d'un utilisateur.
     *
     * @param userId l'ID de l'utilisateur
     * @param newPlanType le nouveau type de plan
     * @return la subscription mise à jour
     */
    @Transactional
    public Subscription changePlan(UUID userId, PlanType newPlanType) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> SereniaException.notFound("Subscription not found for user: " + userId));

        Plan newPlan = planRepository.findByName(newPlanType)
                .orElseThrow(() -> SereniaException.notFound("Plan not found: " + newPlanType));

        subscription.setPlan(newPlan);
        subscriptionRepository.persist(subscription);

        log.info("Changed plan for user {} from {} to {}", 
                userId, subscription.getPlan().getName(), newPlanType);

        return subscription;
    }

    /**
     * Récupère le statut de subscription d'un utilisateur.
     *
     * @param userId l'ID de l'utilisateur
     * @return le DTO avec le statut complet
     */
    public SubscriptionStatusDTO getStatus(UUID userId) {
        Subscription subscription = getOrCreateSubscription(userId);
        Plan plan = subscription.getPlan();

        // Calculer les quotas restants
        int tokensRemaining = Math.max(0, 
                plan.getMonthlyTokenLimit() - subscription.getTokensUsedThisMonth());
        int messagesRemaining = Math.max(0, 
                plan.getDailyMessageLimit() - subscription.getMessagesSentToday());

        // Calculer les dates de reset
        LocalDateTime monthlyResetDate = subscription.getMonthlyPeriodStart().plusMonths(1);
        LocalDateTime dailyResetDate = subscription.getDailyPeriodStart().plusDays(1);

        return new SubscriptionStatusDTO(
                plan.getName().name(),
                tokensRemaining,
                messagesRemaining,
                plan.getPerMessageTokenLimit(),
                plan.getMonthlyTokenLimit(),
                plan.getDailyMessageLimit(),
                subscription.getTokensUsedThisMonth(),
                subscription.getMessagesSentToday(),
                monthlyResetDate,
                dailyResetDate
        );
    }
}
```

---

## 3.5 DTO SubscriptionStatusDTO

### Fichier à créer

`src/main/java/com/lofo/serenia/rest/dto/out/SubscriptionStatusDTO.java`

```java
package com.lofo.serenia.rest.dto.out;

import java.time.LocalDateTime;

/**
 * DTO représentant le statut de subscription d'un utilisateur.
 * Utilisé pour l'observabilité des quotas.
 */
public record SubscriptionStatusDTO(
        String planName,
        int tokensRemainingThisMonth,
        int messagesRemainingToday,
        int perMessageTokenLimit,
        int monthlyTokenLimit,
        int dailyMessageLimit,
        int tokensUsedThisMonth,
        int messagesSentToday,
        LocalDateTime monthlyResetDate,
        LocalDateTime dailyResetDate
) {
}
```

---

## 3.6 Tâches

- [ ] Créer le dossier `src/main/java/com/lofo/serenia/service/subscription/`
- [ ] Créer `QuotaType.java`
- [ ] Créer `QuotaExceededException.java`
- [ ] Créer `TokenEstimationService.java`
- [ ] Créer `QuotaService.java`
- [ ] Créer `SubscriptionService.java`
- [ ] Créer `SubscriptionStatusDTO.java`
- [ ] Vérifier la compilation

---

## 3.7 Diagramme de séquence

```
┌─────────┐     ┌──────────────────┐     ┌─────────────┐     ┌──────────────────────┐
│ Client  │     │ ChatOrchestrator │     │ QuotaService│     │ SubscriptionRepository│
└────┬────┘     └────────┬─────────┘     └──────┬──────┘     └───────────┬──────────┘
     │                   │                      │                        │
     │ sendMessage(msg)  │                      │                        │
     │──────────────────>│                      │                        │
     │                   │                      │                        │
     │                   │ estimateTokens(msg)  │                        │
     │                   │─────────────────────>│                        │
     │                   │      tokens          │                        │
     │                   │<─────────────────────│                        │
     │                   │                      │                        │
     │                   │checkAndReserveQuota()│                        │
     │                   │─────────────────────>│                        │
     │                   │                      │ findByUserIdForUpdate()│
     │                   │                      │───────────────────────>│
     │                   │                      │    subscription (locked)│
     │                   │                      │<───────────────────────│
     │                   │                      │                        │
     │                   │                      │── check rules 1,2,3 ──│
     │                   │                      │                        │
     │                   │                      │ persist(updated)       │
     │                   │                      │───────────────────────>│
     │                   │      OK              │                        │
     │                   │<─────────────────────│                        │
     │                   │                      │                        │
     │                   │──── call LLM ────────│                        │
     │                   │                      │                        │
     │   response        │                      │                        │
     │<──────────────────│                      │                        │
```

---

[← Phase précédente : Repositories](./02-repositories.md) | [Retour au README](./README.md) | [Phase suivante : Intégration →](./04-integration.md)

