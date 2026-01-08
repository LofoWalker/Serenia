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
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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
 * Tests the subscription status and plan change API endpoints.
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

    private static final String SUBSCRIPTION_STATUS_PATH = "/subscription/status";
    private static final String CHANGE_PLAN_PATH = "/subscription/plan";
    private static final String TEST_EMAIL = "subscription-test@example.com";

    private UUID testUserId;
    private Plan freePlan;
    private Plan plusPlan;

    @BeforeEach
    @Transactional
    void setup() {
        RestAssured.baseURI = "http://localhost:8081";
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();

        // Load plans from database (seeded by Liquibase migration)
        freePlan = planRepository.findByName(PlanType.FREE)
                .orElseThrow(() -> new IllegalStateException("FREE plan not found in database"));
        plusPlan = planRepository.findByName(PlanType.PLUS)
                .orElseThrow(() -> new IllegalStateException("PLUS plan not found in database"));

        User testUser = User.builder()
                .email(TEST_EMAIL)
                .password("hashedPassword")
                .firstName("Test")
                .lastName("User")
                .accountActivated(true)
                .role(Role.USER)
                .build();
        userRepository.persist(testUser);
        testUserId = testUser.getId();
    }

    @Test
    @DisplayName("should return 401 when getting status without authentication")
    void should_return_401_when_getting_status_without_auth() {
        given()
                .when()
                .get(SUBSCRIPTION_STATUS_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should return subscription status for authenticated user")
    void should_return_subscription_status_for_authenticated_user() {
        final int tokensUsed = 1500;
        final int messagesSent = 3;

        QuarkusTransaction.requiringNew().run(() -> {
            User user = userRepository.find("id", testUserId).firstResult();
            Plan plan = planRepository.findByName(PlanType.FREE).orElseThrow();
            Subscription subscription = Subscription.builder()
                    .user(user)
                    .plan(plan)
                    .tokensUsedThisMonth(tokensUsed)
                    .messagesSentToday(messagesSent)
                    .monthlyPeriodStart(LocalDateTime.now())
                    .dailyPeriodStart(LocalDateTime.now())
                    .build();
            subscriptionRepository.persist(subscription);
        });

        int expectedTokensRemaining = freePlan.getMonthlyTokenLimit() - tokensUsed;
        int expectedMessagesRemaining = freePlan.getDailyMessageLimit() - messagesSent;

        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, testUserId, "USER");
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(SUBSCRIPTION_STATUS_PATH)
                .then()
                .statusCode(200)
                .body("planName", equalTo("FREE"))
                .body("tokensUsedThisMonth", equalTo(tokensUsed))
                .body("messagesSentToday", equalTo(messagesSent))
                .body("tokensRemainingThisMonth", equalTo(expectedTokensRemaining))
                .body("messagesRemainingToday", equalTo(expectedMessagesRemaining))
                .body("monthlyTokenLimit", equalTo(freePlan.getMonthlyTokenLimit()))
                .body("dailyMessageLimit", equalTo(freePlan.getDailyMessageLimit()))
                .body("monthlyResetDate", notNullValue())
                .body("dailyResetDate", notNullValue());
    }

    @Test
    @DisplayName("should return 404 when subscription does not exist")
    void should_return_404_when_subscription_does_not_exist() {
        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, testUserId, "USER");
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(SUBSCRIPTION_STATUS_PATH)
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("should return correct remaining quotas when near limit")
    void should_return_correct_remaining_quotas_when_near_limit() {
        final int tokensUsed = freePlan.getMonthlyTokenLimit() - 1;
        final int messagesSent = freePlan.getDailyMessageLimit() - 1;

        QuarkusTransaction.requiringNew().run(() -> {
            User user = userRepository.find("id", testUserId).firstResult();
            Plan plan = planRepository.findByName(PlanType.FREE).orElseThrow();
            Subscription subscription = Subscription.builder()
                    .user(user)
                    .plan(plan)
                    .tokensUsedThisMonth(tokensUsed)
                    .messagesSentToday(messagesSent)
                    .monthlyPeriodStart(LocalDateTime.now())
                    .dailyPeriodStart(LocalDateTime.now())
                    .build();
            subscriptionRepository.persist(subscription);
        });

        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, testUserId, "USER");
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
    @DisplayName("should return zero remaining when at limit")
    void should_return_zero_remaining_when_at_limit() {
        final int tokensUsed = freePlan.getMonthlyTokenLimit();
        final int messagesSent = freePlan.getDailyMessageLimit();

        QuarkusTransaction.requiringNew().run(() -> {
            User user = userRepository.find("id", testUserId).firstResult();
            Plan plan = planRepository.findByName(PlanType.FREE).orElseThrow();
            Subscription subscription = Subscription.builder()
                    .user(user)
                    .plan(plan)
                    .tokensUsedThisMonth(tokensUsed)
                    .messagesSentToday(messagesSent)
                    .monthlyPeriodStart(LocalDateTime.now())
                    .dailyPeriodStart(LocalDateTime.now())
                    .build();
            subscriptionRepository.persist(subscription);
        });

        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, testUserId, "USER");
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(SUBSCRIPTION_STATUS_PATH)
                .then()
                .statusCode(200)
                .body("tokensRemainingThisMonth", equalTo(0))
                .body("messagesRemainingToday", equalTo(0));
    }

    // ============== CHANGE PLAN TESTS ==============

    @Test
    @DisplayName("should return 401 when changing plan without authentication")
    void should_return_401_when_changing_plan_without_auth() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"planType\": \"PLUS\"}")
                .when()
                .put(CHANGE_PLAN_PATH)
                .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("should change plan from FREE to PLUS")
    void should_change_plan_from_free_to_plus() {
        final int tokensUsed = 500;
        final int messagesSent = 2;

        QuarkusTransaction.requiringNew().run(() -> {
            User user = userRepository.find("id", testUserId).firstResult();
            Plan plan = planRepository.findByName(PlanType.FREE).orElseThrow();
            Subscription subscription = Subscription.builder()
                    .user(user)
                    .plan(plan)
                    .tokensUsedThisMonth(tokensUsed)
                    .messagesSentToday(messagesSent)
                    .monthlyPeriodStart(LocalDateTime.now())
                    .dailyPeriodStart(LocalDateTime.now())
                    .build();
            subscriptionRepository.persist(subscription);
        });

        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, testUserId, "USER");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"planType\": \"PLUS\"}")
                .when()
                .put(CHANGE_PLAN_PATH)
                .then()
                .statusCode(200)
                .body("planName", equalTo("PLUS"))
                .body("monthlyTokenLimit", equalTo(plusPlan.getMonthlyTokenLimit()))
                .body("dailyMessageLimit", equalTo(plusPlan.getDailyMessageLimit()))
                .body("tokensUsedThisMonth", equalTo(tokensUsed))
                .body("messagesSentToday", equalTo(messagesSent));
    }

    @Test
    @DisplayName("should change plan from PLUS to FREE")
    void should_change_plan_from_plus_to_free() {
        final int tokensUsed = 1000;
        final int messagesSent = 5;

        QuarkusTransaction.requiringNew().run(() -> {
            User user = userRepository.find("id", testUserId).firstResult();
            Plan plan = planRepository.findByName(PlanType.PLUS).orElseThrow();
            Subscription subscription = Subscription.builder()
                    .user(user)
                    .plan(plan)
                    .tokensUsedThisMonth(tokensUsed)
                    .messagesSentToday(messagesSent)
                    .monthlyPeriodStart(LocalDateTime.now())
                    .dailyPeriodStart(LocalDateTime.now())
                    .build();
            subscriptionRepository.persist(subscription);
        });

        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, testUserId, "USER");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"planType\": \"FREE\"}")
                .when()
                .put(CHANGE_PLAN_PATH)
                .then()
                .statusCode(200)
                .body("planName", equalTo("FREE"))
                .body("monthlyTokenLimit", equalTo(freePlan.getMonthlyTokenLimit()))
                .body("dailyMessageLimit", equalTo(freePlan.getDailyMessageLimit()));
    }

    @Test
    @DisplayName("should return 400 when plan type is missing")
    void should_return_400_when_plan_type_is_missing() {
        QuarkusTransaction.requiringNew().run(() -> {
            User user = userRepository.find("id", testUserId).firstResult();
            Plan plan = planRepository.findByName(PlanType.FREE).orElseThrow();
            Subscription subscription = Subscription.builder()
                    .user(user)
                    .plan(plan)
                    .tokensUsedThisMonth(0)
                    .messagesSentToday(0)
                    .monthlyPeriodStart(LocalDateTime.now())
                    .dailyPeriodStart(LocalDateTime.now())
                    .build();
            subscriptionRepository.persist(subscription);
        });

        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, testUserId, "USER");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{}")
                .when()
                .put(CHANGE_PLAN_PATH)
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("should return 404 when subscription does not exist for plan change")
    void should_return_404_when_subscription_does_not_exist_for_plan_change() {
        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, testUserId, "USER");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body("{\"planType\": \"PLUS\"}")
                .when()
                .put(CHANGE_PLAN_PATH)
                .then()
                .statusCode(404);
    }
}
