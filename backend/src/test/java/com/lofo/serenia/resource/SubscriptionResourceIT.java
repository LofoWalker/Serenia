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

    private static final String SUBSCRIPTION_STATUS_PATH = "/api/subscription/status";
    private static final String CHANGE_PLAN_PATH = "/api/subscription/plan";
    private static final String TEST_EMAIL = "subscription-test@example.com";

    private UUID testUserId;
    private UUID freePlanId;
    private UUID plusPlanId;

    @BeforeEach
    @Transactional
    void setup() {
        RestAssured.baseURI = "http://localhost:8081";
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();

        Plan freePlan = planRepository.findByName(PlanType.FREE)
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
        freePlanId = freePlan.getId();

        Plan plusPlan = planRepository.findByName(PlanType.PLUS)
                .orElseGet(() -> {
                    Plan plan = Plan.builder()
                            .name(PlanType.PLUS)
                            .perMessageTokenLimit(4000)
                            .monthlyTokenLimit(100000)
                            .dailyMessageLimit(50)
                            .build();
                    planRepository.persist(plan);
                    return plan;
                });
        plusPlanId = plusPlan.getId();

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
        QuarkusTransaction.requiringNew().run(() -> {
            User user = userRepository.find("id", testUserId).firstResult();
            Plan plan = planRepository.find("id", freePlanId).firstResult();
            Subscription subscription = Subscription.builder()
                    .user(user)
                    .plan(plan)
                    .tokensUsedThisMonth(1500)
                    .messagesSentToday(3)
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
        QuarkusTransaction.requiringNew().run(() -> {
            User user = userRepository.find("id", testUserId).firstResult();
            Plan plan = planRepository.find("id", freePlanId).firstResult();
            Subscription subscription = Subscription.builder()
                    .user(user)
                    .plan(plan)
                    .tokensUsedThisMonth(9999)
                    .messagesSentToday(9)
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
        QuarkusTransaction.requiringNew().run(() -> {
            User user = userRepository.find("id", testUserId).firstResult();
            Plan plan = planRepository.find("id", freePlanId).firstResult();
            Subscription subscription = Subscription.builder()
                    .user(user)
                    .plan(plan)
                    .tokensUsedThisMonth(10000)
                    .messagesSentToday(10)
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
        QuarkusTransaction.requiringNew().run(() -> {
            User user = userRepository.find("id", testUserId).firstResult();
            Plan plan = planRepository.find("id", freePlanId).firstResult();
            Subscription subscription = Subscription.builder()
                    .user(user)
                    .plan(plan)
                    .tokensUsedThisMonth(500)
                    .messagesSentToday(2)
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
                .body("monthlyTokenLimit", equalTo(100000))
                .body("dailyMessageLimit", equalTo(50))
                .body("perMessageTokenLimit", equalTo(4000))
                .body("tokensUsedThisMonth", equalTo(500))
                .body("messagesSentToday", equalTo(2));
    }

    @Test
    @DisplayName("should change plan from PLUS to FREE")
    void should_change_plan_from_plus_to_free() {
        QuarkusTransaction.requiringNew().run(() -> {
            User user = userRepository.find("id", testUserId).firstResult();
            Plan plan = planRepository.find("id", plusPlanId).firstResult();
            Subscription subscription = Subscription.builder()
                    .user(user)
                    .plan(plan)
                    .tokensUsedThisMonth(1000)
                    .messagesSentToday(5)
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
                .body("monthlyTokenLimit", equalTo(10000))
                .body("dailyMessageLimit", equalTo(10));
    }

    @Test
    @DisplayName("should return 400 when plan type is missing")
    void should_return_400_when_plan_type_is_missing() {
        QuarkusTransaction.requiringNew().run(() -> {
            User user = userRepository.find("id", testUserId).firstResult();
            Plan plan = planRepository.find("id", freePlanId).firstResult();
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
