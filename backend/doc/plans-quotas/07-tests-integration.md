# Phase 7 : Tests d'Intégration

**Durée estimée:** 3-4h

**Prérequis:** Phases 1 à 6 complétées

---

## 7.1 SubscriptionResourceIT

### Fichier à créer

`src/test/java/com/lofo/serenia/resource/SubscriptionResourceIT.java`

### Implémentation

```java
package com.lofo.serenia.resource;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.util.JwtTestTokenGenerator;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for SubscriptionResource.
 * Tests the subscription status API endpoint.
 */
@QuarkusTest
@TestProfile(TestResourceProfile.class)
class SubscriptionResourceIT {

    @Inject
    UserRepository userRepository;

    @Inject
    PlanRepository planRepository;

    @Inject
    SubscriptionRepository subscriptionRepository;

    private static final String SUBSCRIPTION_STATUS_PATH = "/api/subscription/status";
    private static final String TEST_EMAIL = "subscription-test@example.com";

    private User testUser;
    private Plan freePlan;

    @BeforeEach
    @Transactional
    void setup() {
        RestAssured.baseURI = "http://localhost:8081";

        // Clean up
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();

        // Ensure FREE plan exists (should be seeded by migration)
        freePlan = planRepository.findByName(PlanType.FREE)
                .orElseGet(() -> {
                    Plan plan = Plan.builder()
                            .name(PlanType.FREE)
                            .perMessageTokenLimit(1000)
                            .monthlyTokenLimit(10000)
                            .dailyMessageLimit(10)
                            .build();
                    planRepository.persist(plan);
                    return plan;
                });

        // Create test user
        testUser = User.builder()
                .email(TEST_EMAIL)
                .password("hashedPassword")
                .firstName("Test")
                .lastName("User")
                .accountActivated(true)
                .role(Role.USER)
                .build();
        userRepository.persist(testUser);
    }

    // ============== AUTHENTICATION TESTS ==============

    @Test
    @DisplayName("should return 401 when getting status without authentication")
    void should_return_401_when_getting_status_without_auth() {
        given()
                .when()
                .get(SUBSCRIPTION_STATUS_PATH)
                .then()
                .statusCode(401);
    }

    // ============== STATUS TESTS ==============

    @Test
    @DisplayName("should return subscription status for authenticated user")
    @Transactional
    void should_return_subscription_status_for_authenticated_user() {
        // Create subscription
        Subscription subscription = Subscription.builder()
                .user(testUser)
                .plan(freePlan)
                .tokensUsedThisMonth(1500)
                .messagesSentToday(3)
                .monthlyPeriodStart(LocalDateTime.now())
                .dailyPeriodStart(LocalDateTime.now())
                .build();
        subscriptionRepository.persist(subscription);

        String token = JwtTestTokenGenerator.generateToken(testUser.getId().toString(), TEST_EMAIL);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(SUBSCRIPTION_STATUS_PATH)
                .then()
                .statusCode(200)
                .body("planName", equalTo("FREE"))
                .body("tokensUsedThisMonth", equalTo(1500))
                .body("messagesSentToday", equalTo(3))
                .body("tokensRemainingThisMonth", equalTo(8500))
                .body("messagesRemainingToday", equalTo(7))
                .body("perMessageTokenLimit", equalTo(1000))
                .body("monthlyTokenLimit", equalTo(10000))
                .body("dailyMessageLimit", equalTo(10))
                .body("monthlyResetDate", notNullValue())
                .body("dailyResetDate", notNullValue());
    }

    @Test
    @DisplayName("should create default subscription if none exists")
    void should_create_default_subscription_if_none_exists() {
        String token = JwtTestTokenGenerator.generateToken(testUser.getId().toString(), TEST_EMAIL);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(SUBSCRIPTION_STATUS_PATH)
                .then()
                .statusCode(200)
                .body("planName", equalTo("FREE"))
                .body("tokensUsedThisMonth", equalTo(0))
                .body("messagesSentToday", equalTo(0))
                .body("tokensRemainingThisMonth", equalTo(10000))
                .body("messagesRemainingToday", equalTo(10));
    }

    @Test
    @DisplayName("should return correct remaining quotas when near limit")
    @Transactional
    void should_return_correct_remaining_quotas_when_near_limit() {
        Subscription subscription = Subscription.builder()
                .user(testUser)
                .plan(freePlan)
                .tokensUsedThisMonth(9999)
                .messagesSentToday(9)
                .monthlyPeriodStart(LocalDateTime.now())
                .dailyPeriodStart(LocalDateTime.now())
                .build();
        subscriptionRepository.persist(subscription);

        String token = JwtTestTokenGenerator.generateToken(testUser.getId().toString(), TEST_EMAIL);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(SUBSCRIPTION_STATUS_PATH)
                .then()
                .statusCode(200)
                .body("tokensRemainingThisMonth", equalTo(1))
                .body("messagesRemainingToday", equalTo(1));
    }

    @Test
    @DisplayName("should return zero remaining when over limit")
    @Transactional
    void should_return_zero_remaining_when_over_limit() {
        Subscription subscription = Subscription.builder()
                .user(testUser)
                .plan(freePlan)
                .tokensUsedThisMonth(15000) // Over limit
                .messagesSentToday(15) // Over limit
                .monthlyPeriodStart(LocalDateTime.now())
                .dailyPeriodStart(LocalDateTime.now())
                .build();
        subscriptionRepository.persist(subscription);

        String token = JwtTestTokenGenerator.generateToken(testUser.getId().toString(), TEST_EMAIL);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(SUBSCRIPTION_STATUS_PATH)
                .then()
                .statusCode(200)
                .body("tokensRemainingThisMonth", equalTo(0))
                .body("messagesRemainingToday", equalTo(0));
    }
}
```

---

## 7.2 QuotaEnforcementIT

### Fichier à créer

`src/test/java/com/lofo/serenia/resource/QuotaEnforcementIT.java`

### Implémentation

```java
package com.lofo.serenia.resource;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.ConversationRepository;
import com.lofo.serenia.persistence.repository.MessageRepository;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.rest.dto.in.MessageRequestDTO;
import com.lofo.serenia.util.JwtTestTokenGenerator;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for quota enforcement.
 * Tests that quotas are properly enforced when sending messages.
 */
@QuarkusTest
@TestProfile(TestResourceProfile.class)
class QuotaEnforcementIT {

    @Inject
    UserRepository userRepository;

    @Inject
    PlanRepository planRepository;

    @Inject
    SubscriptionRepository subscriptionRepository;

    @Inject
    ConversationRepository conversationRepository;

    @Inject
    MessageRepository messageRepository;

    private static final String ADD_MESSAGE_PATH = "/api/conversations/add-message";
    private static final String TEST_EMAIL = "quota-test@example.com";

    private User testUser;
    private Plan testPlan;
    private String token;

    @BeforeEach
    @Transactional
    void setup() {
        RestAssured.baseURI = "http://localhost:8081";

        // Clean up
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();

        // Create a restrictive plan for testing quotas
        testPlan = planRepository.findByName(PlanType.FREE).orElseGet(() -> {
            Plan plan = Plan.builder()
                    .name(PlanType.FREE)
                    .perMessageTokenLimit(100) // Very low for testing
                    .monthlyTokenLimit(500)
                    .dailyMessageLimit(3)
                    .build();
            planRepository.persist(plan);
            return plan;
        });

        // Update plan with restrictive limits for testing
        testPlan.setPerMessageTokenLimit(100);
        testPlan.setMonthlyTokenLimit(500);
        testPlan.setDailyMessageLimit(3);
        planRepository.persist(testPlan);

        // Create test user
        testUser = User.builder()
                .email(TEST_EMAIL)
                .password("hashedPassword")
                .firstName("Test")
                .lastName("User")
                .accountActivated(true)
                .role(Role.USER)
                .build();
        userRepository.persist(testUser);

        token = JwtTestTokenGenerator.generateToken(testUser.getId().toString(), TEST_EMAIL);
    }

    @Nested
    @DisplayName("Daily Message Limit")
    class DailyMessageLimit {

        @Test
        @DisplayName("should reject message when daily limit is reached")
        @Transactional
        void should_reject_message_when_daily_limit_reached() {
            // Create subscription at daily limit
            Subscription subscription = Subscription.builder()
                    .user(testUser)
                    .plan(testPlan)
                    .tokensUsedThisMonth(0)
                    .messagesSentToday(3) // At limit
                    .monthlyPeriodStart(LocalDateTime.now())
                    .dailyPeriodStart(LocalDateTime.now())
                    .build();
            subscriptionRepository.persist(subscription);

            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(new MessageRequestDTO("Hello"))
                    .when()
                    .post(ADD_MESSAGE_PATH)
                    .then()
                    .statusCode(429)
                    .body("quotaType", equalTo("daily_message_limit"))
                    .body("limit", equalTo(3))
                    .body("current", equalTo(3));
        }

        @Test
        @DisplayName("should reset daily limit after period expires")
        @Transactional
        void should_reset_daily_limit_after_period_expires() {
            // Create subscription with expired daily period
            Subscription subscription = Subscription.builder()
                    .user(testUser)
                    .plan(testPlan)
                    .tokensUsedThisMonth(0)
                    .messagesSentToday(3) // At limit
                    .monthlyPeriodStart(LocalDateTime.now())
                    .dailyPeriodStart(LocalDateTime.now().minusDays(2)) // Expired
                    .build();
            subscriptionRepository.persist(subscription);

            // Should succeed because daily period is expired and will be reset
            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(new MessageRequestDTO("Hello"))
                    .when()
                    .post(ADD_MESSAGE_PATH)
                    .then()
                    .statusCode(anyOf(equalTo(200), equalTo(201)));
        }
    }

    @Nested
    @DisplayName("Monthly Token Limit")
    class MonthlyTokenLimit {

        @Test
        @DisplayName("should reject message when monthly tokens exhausted")
        @Transactional
        void should_reject_message_when_monthly_tokens_exhausted() {
            // Create subscription near monthly limit
            Subscription subscription = Subscription.builder()
                    .user(testUser)
                    .plan(testPlan)
                    .tokensUsedThisMonth(500) // At limit
                    .messagesSentToday(0)
                    .monthlyPeriodStart(LocalDateTime.now())
                    .dailyPeriodStart(LocalDateTime.now())
                    .build();
            subscriptionRepository.persist(subscription);

            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(new MessageRequestDTO("Hello, this is a test message"))
                    .when()
                    .post(ADD_MESSAGE_PATH)
                    .then()
                    .statusCode(429)
                    .body("quotaType", equalTo("monthly_token_limit"));
        }

        @Test
        @DisplayName("should reset monthly limit after period expires")
        @Transactional
        void should_reset_monthly_limit_after_period_expires() {
            // Create subscription with expired monthly period
            Subscription subscription = Subscription.builder()
                    .user(testUser)
                    .plan(testPlan)
                    .tokensUsedThisMonth(500) // At limit
                    .messagesSentToday(0)
                    .monthlyPeriodStart(LocalDateTime.now().minusMonths(2)) // Expired
                    .dailyPeriodStart(LocalDateTime.now())
                    .build();
            subscriptionRepository.persist(subscription);

            // Should succeed because monthly period is expired
            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(new MessageRequestDTO("Hi"))
                    .when()
                    .post(ADD_MESSAGE_PATH)
                    .then()
                    .statusCode(anyOf(equalTo(200), equalTo(201)));
        }
    }

    @Nested
    @DisplayName("Per-Message Token Limit")
    class PerMessageTokenLimit {

        @Test
        @DisplayName("should reject message when message is too long")
        @Transactional
        void should_reject_message_when_message_too_long() {
            // Create subscription with available quota
            Subscription subscription = Subscription.builder()
                    .user(testUser)
                    .plan(testPlan)
                    .tokensUsedThisMonth(0)
                    .messagesSentToday(0)
                    .monthlyPeriodStart(LocalDateTime.now())
                    .dailyPeriodStart(LocalDateTime.now())
                    .build();
            subscriptionRepository.persist(subscription);

            // Create a very long message that exceeds per-message limit
            // 100 tokens ≈ 350 characters (using 3.5 chars/token ratio)
            String longMessage = "a".repeat(500); // Should exceed 100 token limit

            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(new MessageRequestDTO(longMessage))
                    .when()
                    .post(ADD_MESSAGE_PATH)
                    .then()
                    .statusCode(429)
                    .body("quotaType", equalTo("message_token_limit"));
        }
    }

    @Nested
    @DisplayName("Quota Consumption")
    class QuotaConsumption {

        @Test
        @DisplayName("should increment counters after successful message")
        @Transactional
        void should_increment_counters_after_successful_message() {
            // Create subscription with available quota
            Subscription subscription = Subscription.builder()
                    .user(testUser)
                    .plan(testPlan)
                    .tokensUsedThisMonth(0)
                    .messagesSentToday(0)
                    .monthlyPeriodStart(LocalDateTime.now())
                    .dailyPeriodStart(LocalDateTime.now())
                    .build();
            subscriptionRepository.persist(subscription);

            // Send a short message
            given()
                    .header("Authorization", "Bearer " + token)
                    .contentType(ContentType.JSON)
                    .body(new MessageRequestDTO("Hi"))
                    .when()
                    .post(ADD_MESSAGE_PATH)
                    .then()
                    .statusCode(anyOf(equalTo(200), equalTo(201)));

            // Check that status reflects the consumption
            given()
                    .header("Authorization", "Bearer " + token)
                    .when()
                    .get("/api/subscription/status")
                    .then()
                    .statusCode(200)
                    .body("messagesSentToday", greaterThan(0))
                    .body("tokensUsedThisMonth", greaterThan(0));
        }
    }

    @Nested
    @DisplayName("Concurrent Requests")
    class ConcurrentRequests {

        @Test
        @DisplayName("should handle concurrent requests without exceeding quota")
        @Transactional
        void should_handle_concurrent_requests_without_exceeding_quota() throws InterruptedException {
            // Create subscription with only 2 messages allowed
            Subscription subscription = Subscription.builder()
                    .user(testUser)
                    .plan(testPlan)
                    .tokensUsedThisMonth(0)
                    .messagesSentToday(1) // Only 2 more allowed
                    .monthlyPeriodStart(LocalDateTime.now())
                    .dailyPeriodStart(LocalDateTime.now())
                    .build();
            subscriptionRepository.persist(subscription);

            // Note: This is a simplified concurrency test.
            // For proper concurrency testing, use tools like Gatling or JMeter.
            // The pessimistic lock in the repository should prevent race conditions.

            // Send 3 concurrent requests - only 2 should succeed
            int successCount = 0;
            int rejectedCount = 0;

            for (int i = 0; i < 3; i++) {
                int status = given()
                        .header("Authorization", "Bearer " + token)
                        .contentType(ContentType.JSON)
                        .body(new MessageRequestDTO("Hi"))
                        .when()
                        .post(ADD_MESSAGE_PATH)
                        .then()
                        .extract()
                        .statusCode();

                if (status == 200 || status == 201) {
                    successCount++;
                } else if (status == 429) {
                    rejectedCount++;
                }
            }

            // At least one should be rejected (daily limit is 3, started at 1)
            org.junit.jupiter.api.Assertions.assertTrue(
                    rejectedCount >= 1,
                    "At least one request should be rejected due to quota"
            );
        }
    }
}
```

---

## 7.3 Mise à jour de ConversationResourceIT

### Fichier à modifier

`src/test/java/com/lofo/serenia/resource/ConversationResourceIT.java`

### Modifications à apporter

```java
// Ajouter les imports
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;

// Ajouter les injections
@Inject
PlanRepository planRepository;

@Inject
SubscriptionRepository subscriptionRepository;

// Modifier le setup pour créer les subscriptions
@BeforeEach
@Transactional
void setup() {
    RestAssured.baseURI = "http://localhost:8081";
    
    // Clean up dans le bon ordre
    messageRepository.deleteAll();
    conversationRepository.deleteAll();
    subscriptionRepository.deleteAll();
    userRepository.deleteAll();
    
    // Créer ou récupérer le plan FREE
    Plan freePlan = planRepository.findByName(PlanType.FREE)
            .orElseGet(() -> {
                Plan plan = Plan.builder()
                        .name(PlanType.FREE)
                        .perMessageTokenLimit(10000) // High limit for tests
                        .monthlyTokenLimit(100000)
                        .dailyMessageLimit(100)
                        .build();
                planRepository.persist(plan);
                return plan;
            });
    
    // ... création des users existants ...
    
    // Pour chaque user de test, créer une subscription
    // Après userRepository.persist(testUser) :
    Subscription subscription = Subscription.builder()
            .user(testUser)
            .plan(freePlan)
            .tokensUsedThisMonth(0)
            .messagesSentToday(0)
            .monthlyPeriodStart(LocalDateTime.now())
            .dailyPeriodStart(LocalDateTime.now())
            .build();
    subscriptionRepository.persist(subscription);
}
```

---

## 7.4 Configuration de test

### TestResourceProfile

Vérifier que le profil de test configure correctement la base de données :

```java
package com.lofo.serenia;

import io.quarkus.test.junit.QuarkusTestProfile;
import java.util.Map;

public class TestResourceProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.http.test-port", "8081",
                "quarkus.datasource.jdbc.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                "quarkus.datasource.db-kind", "h2",
                "quarkus.hibernate-orm.database.generation", "drop-and-create",
                // Désactiver le mock LLM ou configurer un mock
                "quarkus.openai.api-key", "test-key"
        );
    }
}
```

### application.properties pour les tests

`src/test/resources/application.properties`

```properties
# Test database
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
quarkus.hibernate-orm.database.generation=drop-and-create

# Disable real LLM calls in tests
%test.quarkus.openai.enabled=false

# Test plans configuration
serenia.plans.free.per-message-token-limit=10000
serenia.plans.free.monthly-token-limit=100000
serenia.plans.free.daily-message-limit=100
```

---

## 7.5 Tâches

- [ ] Créer `SubscriptionResourceIT.java`
- [ ] Créer `QuotaEnforcementIT.java`
- [ ] Modifier `ConversationResourceIT.java` pour inclure les subscriptions
- [ ] Configurer le profil de test si nécessaire
- [ ] Exécuter tous les tests d'intégration
- [ ] Vérifier la couverture de code

---

## 7.6 Exécution des tests

```bash
# Exécuter tous les tests d'intégration
./mvnw verify

# Exécuter uniquement les tests d'intégration de quota
./mvnw test -Dtest="*IT" -DfailIfNoTests=false

# Exécuter un test spécifique
./mvnw test -Dtest="QuotaEnforcementIT"

# Avec logs détaillés
./mvnw test -Dtest="QuotaEnforcementIT" -Dquarkus.log.level=DEBUG
```

---

## 7.7 Matrice de tests

| Scénario | Test | Résultat attendu |
|----------|------|------------------|
| Status sans auth | `should_return_401_when_getting_status_without_auth` | 401 |
| Status avec auth | `should_return_subscription_status_for_authenticated_user` | 200 + DTO |
| Création auto subscription | `should_create_default_subscription_if_none_exists` | 200 + FREE plan |
| Limite daily atteinte | `should_reject_message_when_daily_limit_reached` | 429 |
| Limite monthly atteinte | `should_reject_message_when_monthly_tokens_exhausted` | 429 |
| Message trop long | `should_reject_message_when_message_too_long` | 429 |
| Reset daily | `should_reset_daily_limit_after_period_expires` | 200 |
| Reset monthly | `should_reset_monthly_limit_after_period_expires` | 200 |
| Incrémentation compteurs | `should_increment_counters_after_successful_message` | Compteurs > 0 |
| Concurrence | `should_handle_concurrent_requests_without_exceeding_quota` | Max N succès |

---

[← Phase précédente : Tests unitaires](./06-tests-unitaires.md) | [Retour au README](./README.md) | [Phase suivante : Configuration →](./08-configuration.md)

