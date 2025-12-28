# Phase 2 : Couche Repository

**Durée estimée:** 1-2h

**Prérequis:** Phase 1 complétée (entités et migration)

---

## 2.1 PlanRepository

### Fichier à créer

`src/main/java/com/lofo/serenia/persistence/repository/PlanRepository.java`

### Implémentation

```java
package com.lofo.serenia.persistence.repository;

import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * Repository pour l'accès aux plans d'abonnement.
 */
@ApplicationScoped
public class PlanRepository implements PanacheRepository<Plan> {

    /**
     * Recherche un plan par son type.
     *
     * @param planType le type de plan (FREE, PLUS, MAX)
     * @return le plan correspondant ou empty
     */
    public Optional<Plan> findByName(PlanType planType) {
        return find("name", planType).firstResultOptional();
    }

    /**
     * Recherche un plan par son nom (String).
     *
     * @param name le nom du plan
     * @return le plan correspondant ou empty
     */
    public Optional<Plan> findByName(String name) {
        return find("name", PlanType.valueOf(name)).firstResultOptional();
    }

    /**
     * Récupère le plan FREE par défaut.
     *
     * @return le plan FREE
     * @throws IllegalStateException si le plan FREE n'existe pas
     */
    public Plan getFreePlan() {
        return findByName(PlanType.FREE)
                .orElseThrow(() -> new IllegalStateException("Plan FREE not found in database"));
    }
}
```

---

## 2.2 SubscriptionRepository

### Fichier à créer

`src/main/java/com/lofo/serenia/persistence/repository/SubscriptionRepository.java`

### Implémentation

```java
package com.lofo.serenia.persistence.repository;

import com.lofo.serenia.persistence.entity.subscription.Subscription;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour l'accès aux subscriptions utilisateurs.
 * Fournit des méthodes atomiques pour la gestion des quotas.
 */
@ApplicationScoped
public class SubscriptionRepository implements PanacheRepository<Subscription> {

    /**
     * Recherche une subscription par ID utilisateur.
     *
     * @param userId l'ID de l'utilisateur
     * @return la subscription ou empty
     */
    public Optional<Subscription> findByUserId(UUID userId) {
        return find("user.id", userId).firstResultOptional();
    }

    /**
     * Recherche une subscription avec verrouillage pessimiste.
     * Utilisé pour les opérations atomiques de mise à jour des quotas.
     *
     * @param userId l'ID de l'utilisateur
     * @return la subscription verrouillée ou empty
     */
    public Optional<Subscription> findByUserIdForUpdate(UUID userId) {
        return find("user.id", userId)
                .withLock(LockModeType.PESSIMISTIC_WRITE)
                .firstResultOptional();
    }

    /**
     * Incrémente atomiquement le compteur de tokens utilisés.
     * Utilise une requête UPDATE native pour garantir l'atomicité.
     *
     * @param subscriptionId l'ID de la subscription
     * @param tokens le nombre de tokens à ajouter
     * @return le nombre de lignes affectées (1 si succès)
     */
    @Transactional
    public int incrementTokensUsed(UUID subscriptionId, int tokens) {
        return update("tokensUsedThisMonth = tokensUsedThisMonth + ?1, updatedAt = ?2 WHERE id = ?3",
                tokens, LocalDateTime.now(), subscriptionId);
    }

    /**
     * Incrémente atomiquement le compteur de messages envoyés.
     *
     * @param subscriptionId l'ID de la subscription
     * @return le nombre de lignes affectées (1 si succès)
     */
    @Transactional
    public int incrementMessagesSent(UUID subscriptionId) {
        return update("messagesSentToday = messagesSentToday + 1, updatedAt = ?1 WHERE id = ?2",
                LocalDateTime.now(), subscriptionId);
    }

    /**
     * Incrémente atomiquement les deux compteurs en une seule opération.
     *
     * @param subscriptionId l'ID de la subscription
     * @param tokens le nombre de tokens à ajouter
     * @return le nombre de lignes affectées (1 si succès)
     */
    @Transactional
    public int incrementUsage(UUID subscriptionId, int tokens) {
        return update("tokensUsedThisMonth = tokensUsedThisMonth + ?1, " +
                        "messagesSentToday = messagesSentToday + 1, " +
                        "updatedAt = ?2 WHERE id = ?3",
                tokens, LocalDateTime.now(), subscriptionId);
    }

    /**
     * Réinitialise le compteur journalier pour les subscriptions dont la période est expirée.
     *
     * @param beforeDate la date limite (subscriptions avec daily_period_start avant cette date)
     * @return le nombre de subscriptions réinitialisées
     */
    @Transactional
    public int resetDailyCounters(LocalDateTime beforeDate) {
        return update("messagesSentToday = 0, dailyPeriodStart = ?1, updatedAt = ?1 " +
                        "WHERE dailyPeriodStart < ?2",
                LocalDateTime.now(), beforeDate);
    }

    /**
     * Réinitialise le compteur mensuel pour les subscriptions dont la période est expirée.
     *
     * @param beforeDate la date limite
     * @return le nombre de subscriptions réinitialisées
     */
    @Transactional
    public int resetMonthlyCounters(LocalDateTime beforeDate) {
        return update("tokensUsedThisMonth = 0, monthlyPeriodStart = ?1, updatedAt = ?1 " +
                        "WHERE monthlyPeriodStart < ?2",
                LocalDateTime.now(), beforeDate);
    }

    /**
     * Réinitialise le compteur journalier pour une subscription spécifique.
     *
     * @param subscriptionId l'ID de la subscription
     * @return le nombre de lignes affectées
     */
    @Transactional
    public int resetDailyCounter(UUID subscriptionId) {
        return update("messagesSentToday = 0, dailyPeriodStart = ?1, updatedAt = ?1 WHERE id = ?2",
                LocalDateTime.now(), subscriptionId);
    }

    /**
     * Réinitialise le compteur mensuel pour une subscription spécifique.
     *
     * @param subscriptionId l'ID de la subscription
     * @return le nombre de lignes affectées
     */
    @Transactional
    public int resetMonthlyCounter(UUID subscriptionId) {
        return update("tokensUsedThisMonth = 0, monthlyPeriodStart = ?1, updatedAt = ?1 WHERE id = ?2",
                LocalDateTime.now(), subscriptionId);
    }

    /**
     * Vérifie si un utilisateur a déjà une subscription.
     *
     * @param userId l'ID de l'utilisateur
     * @return true si une subscription existe
     */
    public boolean existsByUserId(UUID userId) {
        return count("user.id", userId) > 0;
    }
}
```

---

## 2.3 Notes techniques

### Verrouillage pessimiste

Le verrouillage pessimiste (`PESSIMISTIC_WRITE`) est utilisé pour éviter les race conditions lors de la vérification et mise à jour des quotas :

```java
// Exemple d'utilisation
Subscription sub = subscriptionRepository.findByUserIdForUpdate(userId)
    .orElseThrow(...);

// À ce stade, la ligne est verrouillée jusqu'à la fin de la transaction
// Les autres transactions attendront
```

### Opérations atomiques

Les méthodes `incrementTokensUsed`, `incrementMessagesSent` et `incrementUsage` utilisent des requêtes UPDATE directes pour garantir l'atomicité :

```sql
-- Pas de risque de read → compute → write
UPDATE subscriptions 
SET tokens_used_this_month = tokens_used_this_month + :tokens
WHERE id = :id
```

### Alternative : Requête avec condition

Pour une sécurité maximale, on peut combiner vérification et mise à jour :

```java
/**
 * Incrémente les compteurs SEULEMENT si les quotas ne sont pas dépassés.
 * Retourne 0 si les quotas seraient dépassés.
 */
@Transactional
public int incrementUsageIfAllowed(UUID subscriptionId, int tokens, 
        int maxMonthlyTokens, int maxDailyMessages) {
    return getEntityManager()
        .createQuery("UPDATE Subscription s SET " +
            "s.tokensUsedThisMonth = s.tokensUsedThisMonth + :tokens, " +
            "s.messagesSentToday = s.messagesSentToday + 1, " +
            "s.updatedAt = :now " +
            "WHERE s.id = :id " +
            "AND s.tokensUsedThisMonth + :tokens <= :maxTokens " +
            "AND s.messagesSentToday < :maxMessages")
        .setParameter("tokens", tokens)
        .setParameter("now", LocalDateTime.now())
        .setParameter("id", subscriptionId)
        .setParameter("maxTokens", maxMonthlyTokens)
        .setParameter("maxMessages", maxDailyMessages)
        .executeUpdate();
}
```

---

## 2.4 Tâches

- [ ] Créer `PlanRepository.java`
- [ ] Créer `SubscriptionRepository.java`
- [ ] Écrire les tests unitaires des repositories (optionnel, sera couvert en Phase 6)
- [ ] Vérifier que les méthodes fonctionnent avec une base de test

---

## 2.5 Validation

```java
// Test rapide dans un @QuarkusTest
@Inject
PlanRepository planRepository;

@Inject
SubscriptionRepository subscriptionRepository;

@Test
void should_find_free_plan() {
    Plan free = planRepository.getFreePlan();
    assertNotNull(free);
    assertEquals(PlanType.FREE, free.getName());
}
```

---

[← Phase précédente : Modèle de données](./01-modele-donnees.md) | [Retour au README](./README.md) | [Phase suivante : Services →](./03-services.md)

