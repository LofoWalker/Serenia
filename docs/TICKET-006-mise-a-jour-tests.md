# TICKET-006 : Mettre à jour les tests unitaires

## Contexte
Les modifications des tickets précédents cassent les tests existants. Ce ticket couvre la mise à jour des tests et l'ajout de nouveaux tests pour la normalisation.

## Objectif
- Mettre à jour les tests existants pour les nouvelles signatures
- Ajouter des tests unitaires pour la formule de normalisation

## Fichiers à modifier

### 1. QuotaServiceTest.java
`backend/src/test/java/com/lofo/serenia/service/subscription/QuotaServiceTest.java`

#### Tests à modifier

**Test `should_increment_counters_with_actual_tokens`**
```java
// Avant
@Test
@DisplayName("should increment counters with actual tokens")
void should_increment_counters_with_actual_tokens() {
    int actualTokensUsed = 432;
    when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
            .thenReturn(Optional.of(subscription));
    quotaService.recordUsage(USER_ID, actualTokensUsed);
    assertEquals(actualTokensUsed, subscription.getTokensUsedThisMonth());
    // ...
}

// Après
@Test
@DisplayName("should increment counters with normalized tokens")
void should_increment_counters_with_normalized_tokens() {
    // promptTokens=500, cachedTokens=0, completionTokens=100
    // normalized = 500 + 0 + 400 = 900
    when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
            .thenReturn(Optional.of(subscription));
    
    quotaService.recordUsage(USER_ID, 500, 0, 100);
    
    assertEquals(900, subscription.getTokensUsedThisMonth());
    assertEquals(1, subscription.getMessagesSentToday());
    verify(subscriptionRepository).persist(subscription);
}
```

**Test `should_throw_when_subscription_not_found`**
```java
// Avant
assertThrows(IllegalStateException.class,
        () -> quotaService.recordUsage(USER_ID, 100));

// Après
assertThrows(IllegalStateException.class,
        () -> quotaService.recordUsage(USER_ID, 100, 0, 50));
```

**Test `should_accumulate_tokens_on_multiple_calls`**
```java
// Après
@Test
@DisplayName("should accumulate normalized tokens on multiple calls")
void should_accumulate_tokens_on_multiple_calls() {
    when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
            .thenReturn(Optional.of(subscription));

    // Appel 1: prompt=200, cached=0, completion=25 → normalized = 200 + 0 + 100 = 300
    quotaService.recordUsage(USER_ID, 200, 0, 25);
    assertEquals(300, subscription.getTokensUsedThisMonth());
    assertEquals(1, subscription.getMessagesSentToday());

    // Appel 2: prompt=300, cached=200, completion=50 → normalized = 100 + 100 + 200 = 400
    quotaService.recordUsage(USER_ID, 300, 200, 50);
    assertEquals(700, subscription.getTokensUsedThisMonth());
    assertEquals(2, subscription.getMessagesSentToday());
    
    verify(subscriptionRepository, times(2)).persist(subscription);
}
```

#### Nouveau test à ajouter : Normalisation des tokens

```java
@Nested
@DisplayName("Token Normalization")
class TokenNormalization {

    @Test
    @DisplayName("should normalize tokens without cache")
    void should_normalize_tokens_without_cache() {
        // prompt=1000, cached=0, completion=100
        // normalized = 1000 + 0 + 400 = 1400
        when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(subscription));
        
        quotaService.recordUsage(USER_ID, 1000, 0, 100);
        
        assertEquals(1400, subscription.getTokensUsedThisMonth());
    }

    @Test
    @DisplayName("should normalize tokens with partial cache")
    void should_normalize_tokens_with_partial_cache() {
        // prompt=1000, cached=800, completion=100
        // normalized = (1000-800) + (800/2) + (100*4) = 200 + 400 + 400 = 1000
        when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(subscription));
        
        quotaService.recordUsage(USER_ID, 1000, 800, 100);
        
        assertEquals(1000, subscription.getTokensUsedThisMonth());
    }

    @Test
    @DisplayName("should normalize tokens with full cache")
    void should_normalize_tokens_with_full_cache() {
        // prompt=500, cached=500, completion=50
        // normalized = 0 + 250 + 200 = 450
        when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(subscription));
        
        quotaService.recordUsage(USER_ID, 500, 500, 50);
        
        assertEquals(450, subscription.getTokensUsedThisMonth());
    }

    @Test
    @DisplayName("should handle output only")
    void should_handle_output_only() {
        // prompt=0, cached=0, completion=100
        // normalized = 0 + 0 + 400 = 400
        when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(subscription));
        
        quotaService.recordUsage(USER_ID, 0, 0, 100);
        
        assertEquals(400, subscription.getTokensUsedThisMonth());
    }
}
```

### 2. ChatOrchestratorTest.java
`backend/src/test/java/com/lofo/serenia/service/chat/ChatOrchestratorTest.java`

#### Test à modifier

```java
// Avant
ChatCompletionService.ChatCompletionResult completionResult =
        new ChatCompletionService.ChatCompletionResult("Assistant reply", 432);
// ...
verify(quotaService).recordUsage(FIXED_USER_ID, 432);

// Après
ChatCompletionService.ChatCompletionResult completionResult =
        new ChatCompletionService.ChatCompletionResult("Assistant reply", 500, 100, 50);
// ...
verify(quotaService).recordUsage(FIXED_USER_ID, 500, 100, 50);
```

## Critères d'acceptation
- [ ] Tous les tests existants sont mis à jour
- [ ] Les nouveaux tests de normalisation sont ajoutés
- [ ] Tous les tests passent (`mvn test`)
- [ ] Les tests couvrent les cas : sans cache, cache partiel, cache total, output seul

## Dépendances
- TICKET-001 (record modifié)
- TICKET-003 (normalizeTokens)
- TICKET-004 (recordUsage)
- TICKET-005 (ChatOrchestrator)

## Bloque
- Aucun (ce ticket finalise l'implémentation)

