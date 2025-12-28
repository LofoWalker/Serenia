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
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();
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
    @Transactional
    void should_return_subscription_status_for_authenticated_user() {
        Subscription subscription = Subscription.builder()
                .user(testUser)
                .plan(freePlan)
                .tokensUsedThisMonth(1500)
                .messagesSentToday(3)
                .monthlyPeriodStart(LocalDateTime.now())
                .dailyPeriodStart(LocalDateTime.now())
                .build();
        subscriptionRepository.persist(subscription);
        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, testUser.getId(), "USER");
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
        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, testUser.getId(), "USER");
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
        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, testUser.getId(), "USER");
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
    @Transactional
    void should_return_zero_remaining_when_at_limit() {
        Subscription subscription = Subscription.builder()
                .user(testUser)
                .plan(freePlan)
                .tokensUsedThisMonth(10000)
                .messagesSentToday(10)
                .monthlyPeriodStart(LocalDateTime.now())
                .dailyPeriodStart(LocalDateTime.now())
                .build();
        subscriptionRepository.persist(subscription);
        String token = JwtTestTokenGenerator.generateToken(TEST_EMAIL, testUser.getId(), "USER");
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
