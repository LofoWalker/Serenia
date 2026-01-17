package com.lofo.serenia.resource;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.config.StripeConfig;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.subscription.SubscriptionStatus;
import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.persistence.repository.UserRepository;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestProfile(TestResourceProfile.class)
@DisplayName("StripeWebhookResource Integration Tests")
class StripeWebhookResourceIT {

    @Inject
    UserRepository userRepository;

    @Inject
    SubscriptionRepository subscriptionRepository;

    @Inject
    PlanRepository planRepository;

    @Inject
    StripeConfig stripeConfig;

    private String stripeCustomerId;
    private static final String TEST_EMAIL = "stripe-webhook-user@example.com";
    private static final String WEBHOOK_PATH = "/stripe/webhook";

    @BeforeEach
    @Transactional
    void setup() {
        RestAssured.baseURI = "http://localhost:8081";
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();

        stripeCustomerId = "cus_test_" + UUID.randomUUID().toString().substring(0, 8);

        User user = User.builder()
                .email(TEST_EMAIL)
                .password("hashedpassword")
                .firstName("Stripe")
                .lastName("User")
                .accountActivated(true)
                .role(Role.USER)
                .build();
        userRepository.persist(user);

        Plan freePlan = planRepository.findByName(PlanType.FREE)
                .orElseThrow(() -> new IllegalStateException("FREE plan not found"));
        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(freePlan)
                .status(SubscriptionStatus.ACTIVE)
                .stripeCustomerId(stripeCustomerId)
                .tokensUsedThisMonth(0)
                .messagesSentToday(0)
                .monthlyPeriodStart(LocalDateTime.now())
                .dailyPeriodStart(LocalDateTime.now())
                .build();
        subscriptionRepository.persist(subscription);
    }

    @Nested
    @DisplayName("Valid events")
    class ValidEvents {

        @Test
        @DisplayName("should process checkout session completed")
        void should_process_checkout_session_completed() {
            String payload = createCheckoutSessionCompletedPayload();
            String signature = generateStripeSignature(payload);

            given()
                    .contentType(ContentType.JSON)
                    .header("Stripe-Signature", signature)
                    .body(payload)
                    .when()
                    .post(WEBHOOK_PATH)
                    .then()
                    .statusCode(200)
                    .body("received", equalTo(true));
        }

        @Test
        @DisplayName("should process subscription created")
        void should_process_subscription_created() {
            String payload = createSubscriptionCreatedPayload();
            String signature = generateStripeSignature(payload);

            given()
                    .contentType(ContentType.JSON)
                    .header("Stripe-Signature", signature)
                    .body(payload)
                    .when()
                    .post(WEBHOOK_PATH)
                    .then()
                    .statusCode(200);
        }

        @Test
        @DisplayName("should process invoice paid")
        void should_process_invoice_paid() {
            String payload = createInvoicePaidPayload();
            String signature = generateStripeSignature(payload);

            given()
                    .contentType(ContentType.JSON)
                    .header("Stripe-Signature", signature)
                    .body(payload)
                    .when()
                    .post(WEBHOOK_PATH)
                    .then()
                    .statusCode(200);
        }
    }

    @Nested
    @DisplayName("Error cases")
    class ErrorCases {

        @Test
        @DisplayName("should return 400 for invalid signature")
        void should_return_400_for_invalid_signature() {
            String payload = createCheckoutSessionCompletedPayload();
            String invalidSignature = "t=12345,v1=invalid_signature";

            given()
                    .contentType(ContentType.JSON)
                    .header("Stripe-Signature", invalidSignature)
                    .body(payload)
                    .when()
                    .post(WEBHOOK_PATH)
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("should return 400 when signature header missing")
        void should_return_400_when_signature_header_missing() {
            String payload = createCheckoutSessionCompletedPayload();

            given()
                    .contentType(ContentType.JSON)
                    .body(payload)
                    .when()
                    .post(WEBHOOK_PATH)
                    .then()
                    .statusCode(400);
        }

        @Test
        @DisplayName("should return 200 for unknown event type")
        void should_return_200_for_unknown_event_type() {
            String payload = createUnknownEventPayload();
            String signature = generateStripeSignature(payload);

            given()
                    .contentType(ContentType.JSON)
                    .header("Stripe-Signature", signature)
                    .body(payload)
                    .when()
                    .post(WEBHOOK_PATH)
                    .then()
                    .statusCode(200);
        }
    }

    private String createCheckoutSessionCompletedPayload() {
        String eventId = UUID.randomUUID().toString().substring(0, 8);
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        String subId = UUID.randomUUID().toString().substring(0, 8);
        return "{\"id\": \"evt_test_" + eventId + "\", " +
                "\"api_version\": \"2024-12-18.acacia\", " +
                "\"type\": \"checkout.session.completed\", " +
                "\"data\": {\"object\": {" +
                "\"id\": \"cs_test_" + sessionId + "\", " +
                "\"object\": \"checkout.session\", " +
                "\"customer\": \"" + stripeCustomerId + "\", " +
                "\"subscription\": \"sub_test_" + subId + "\", " +
                "\"mode\": \"subscription\"}}}";
    }

    private String createSubscriptionCreatedPayload() {
        String eventId = UUID.randomUUID().toString().substring(0, 8);
        String subId = UUID.randomUUID().toString().substring(0, 8);
        return "{\"id\": \"evt_test_" + eventId + "\", " +
                "\"api_version\": \"2024-12-18.acacia\", " +
                "\"type\": \"customer.subscription.created\", " +
                "\"data\": {\"object\": {" +
                "\"id\": \"sub_test_" + subId + "\", " +
                "\"object\": \"subscription\", " +
                "\"customer\": \"" + stripeCustomerId + "\", " +
                "\"status\": \"active\"}}}";
    }

    private String createInvoicePaidPayload() {
        String eventId = UUID.randomUUID().toString().substring(0, 8);
        String invoiceId = UUID.randomUUID().toString().substring(0, 8);
        String subId = UUID.randomUUID().toString().substring(0, 8);
        return "{\"id\": \"evt_test_" + eventId + "\", " +
                "\"api_version\": \"2024-12-18.acacia\", " +
                "\"type\": \"invoice.paid\", " +
                "\"data\": {\"object\": {" +
                "\"id\": \"in_test_" + invoiceId + "\", " +
                "\"object\": \"invoice\", " +
                "\"customer\": \"" + stripeCustomerId + "\", " +
                "\"subscription\": \"sub_test_" + subId + "\"}}}";
    }

    private String createUnknownEventPayload() {
        String eventId = UUID.randomUUID().toString().substring(0, 8);
        return "{\"id\": \"evt_test_" + eventId + "\", " +
                "\"api_version\": \"2024-12-18.acacia\", " +
                "\"type\": \"unknown.event.type\", " +
                "\"data\": {\"object\": {}}}";
    }

    private String generateStripeSignature(String payload) {
        try {
            long timestamp = System.currentTimeMillis() / 1000;
            String signedPayload = timestamp + "." + payload;

            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    stripeConfig.webhookSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return String.format("t=%d,v1=%s", timestamp, hexString);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Stripe signature", e);
        }
    }
}
