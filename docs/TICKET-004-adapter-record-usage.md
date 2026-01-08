# TICKET-004 : Adapter QuotaService.recordUsage()

## Contexte
La méthode `recordUsage` doit recevoir les 3 valeurs brutes de tokens, appliquer la normalisation, logger les deux versions et stocker la valeur normalisée.

## Objectif
Modifier la signature et le comportement de `recordUsage()` pour intégrer la normalisation des tokens.

## Fichier à modifier
`backend/src/main/java/com/lofo/serenia/service/subscription/QuotaService.java`

## Modification

### Avant
```java
/**
 * Records the actual token usage returned by the OpenAI API.
 *
 * @param userId the user identifier
 * @param actualTokensUsed the actual tokens consumed (from ChatCompletion.usage().totalTokens())
 */
@Transactional
public void recordUsage(UUID userId, int actualTokensUsed) {
    Subscription subscription = getSubscriptionForUpdate(userId);

    updateUsageCounters(subscription, actualTokensUsed);

    subscriptionRepository.persist(subscription);

    log.debug("Recorded usage for user {}: {} tokens (total: {}), {} messages today",
            userId, actualTokensUsed, subscription.getTokensUsedThisMonth(),
            subscription.getMessagesSentToday());
}
```

### Après
```java
/**
 * Records the token usage with cost normalization.
 * Raw tokens are logged for monitoring, normalized tokens are stored for billing.
 *
 * @param userId the user identifier
 * @param promptTokens total input tokens (from usage.promptTokens())
 * @param cachedTokens cached input tokens (from usage.promptTokensDetails().cachedTokens())
 * @param completionTokens output tokens (from usage.completionTokens())
 */
@Transactional
public void recordUsage(UUID userId, int promptTokens, int cachedTokens, int completionTokens) {
    Subscription subscription = getSubscriptionForUpdate(userId);

    int normalizedTokens = normalizeTokens(promptTokens, cachedTokens, completionTokens);

    updateUsageCounters(subscription, normalizedTokens);

    subscriptionRepository.persist(subscription);

    log.info("Token usage for user {} - Raw [prompt: {}, cached: {}, completion: {}] | Normalized: {} | Monthly total: {}",
            userId, promptTokens, cachedTokens, completionTokens, 
            normalizedTokens, subscription.getTokensUsedThisMonth());
}
```

## Méthode privée à ajouter
Voir TICKET-003 pour la méthode `normalizeTokens()`.

## Logging
Le log doit contenir :
- Les 3 valeurs brutes (prompt, cached, completion)
- La valeur normalisée
- Le total mensuel après mise à jour

Niveau de log : `INFO` (pour pouvoir monitorer l'efficacité du cache en production)

## Critères d'acceptation
- [ ] La signature prend 3 paramètres : `promptTokens`, `cachedTokens`, `completionTokens`
- [ ] La normalisation est appliquée avant stockage
- [ ] Le log contient les valeurs brutes ET normalisées
- [ ] La Javadoc est mise à jour
- [ ] Les tests existants sont adaptés (voir TICKET-006)

## Dépendances
- TICKET-003 (méthode normalizeTokens)

## Bloque
- TICKET-005 (ChatOrchestrator)

