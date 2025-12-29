package com.lofo.serenia.service.subscription;

import com.lofo.serenia.config.StripeConfig;
import com.lofo.serenia.exception.exceptions.SereniaException;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.service.user.shared.UserFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeService Tests")
class StripeServiceTest {

    @Mock
    private StripeConfig stripeConfig;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private UserFinder userFinder;

    private StripeService stripeService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String STRIPE_CUSTOMER_ID = "cus_test123";
    private static final String STRIPE_PRICE_ID = "price_test123";

    private User user;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        stripeService = new StripeService(stripeConfig, subscriptionRepository, planRepository, userFinder);

        user = User.builder()
                .id(USER_ID)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        Plan freePlan = Plan.builder()
                .id(UUID.randomUUID())
                .name(PlanType.FREE)
                .priceCents(0)
                .currency("EUR")
                .build();

        subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(freePlan)
                .build();
    }

    @Nested
    @DisplayName("createCheckoutSession")
    class CreateCheckoutSession {

        @Test
        @DisplayName("should throw when plan is FREE")
        void should_throw_when_plan_is_free() {
            SereniaException exception = assertThrows(
                    SereniaException.class,
                    () -> stripeService.createCheckoutSession(USER_ID, PlanType.FREE)
            );

            assertEquals(400, exception.getHttpStatus());
            assertTrue(exception.getMessage().contains("Cannot checkout for FREE plan"));
        }

        @Test
        @DisplayName("should throw when user not found")
        void should_throw_when_user_not_found() {
            when(userFinder.findByIdOrThrow(USER_ID))
                    .thenThrow(SereniaException.notFound("User not found"));

            SereniaException exception = assertThrows(
                    SereniaException.class,
                    () -> stripeService.createCheckoutSession(USER_ID, PlanType.PLUS)
            );

            assertEquals(404, exception.getHttpStatus());
        }

        @Test
        @DisplayName("should throw when subscription not found")
        void should_throw_when_subscription_not_found() {
            when(userFinder.findByIdOrThrow(USER_ID)).thenReturn(user);
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            SereniaException exception = assertThrows(
                    SereniaException.class,
                    () -> stripeService.createCheckoutSession(USER_ID, PlanType.PLUS)
            );

            assertEquals(404, exception.getHttpStatus());
            assertTrue(exception.getMessage().contains("Subscription not found"));
        }

        @Test
        @DisplayName("should throw when plan not found")
        void should_throw_when_plan_not_found() {
            when(userFinder.findByIdOrThrow(USER_ID)).thenReturn(user);
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(subscription));
            when(planRepository.findByName(PlanType.PLUS)).thenReturn(Optional.empty());

            SereniaException exception = assertThrows(
                    SereniaException.class,
                    () -> stripeService.createCheckoutSession(USER_ID, PlanType.PLUS)
            );

            assertEquals(404, exception.getHttpStatus());
            assertTrue(exception.getMessage().contains("Plan not found"));
        }

        @Test
        @DisplayName("should throw when plan has no Stripe price ID")
        void should_throw_when_plan_has_no_stripe_price_id() {
            Plan planWithoutStripeId = Plan.builder()
                    .id(UUID.randomUUID())
                    .name(PlanType.PLUS)
                    .stripePriceId(null)
                    .build();

            when(userFinder.findByIdOrThrow(USER_ID)).thenReturn(user);
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(subscription));
            when(planRepository.findByName(PlanType.PLUS)).thenReturn(Optional.of(planWithoutStripeId));

            SereniaException exception = assertThrows(
                    SereniaException.class,
                    () -> stripeService.createCheckoutSession(USER_ID, PlanType.PLUS)
            );

            assertEquals(400, exception.getHttpStatus());
            assertTrue(exception.getMessage().contains("no Stripe Price ID"));
        }
    }

    @Nested
    @DisplayName("createPortalSession")
    class CreatePortalSession {

        @Test
        @DisplayName("should throw when subscription not found")
        void should_throw_when_subscription_not_found() {
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            SereniaException exception = assertThrows(
                    SereniaException.class,
                    () -> stripeService.createPortalSession(USER_ID)
            );

            assertEquals(404, exception.getHttpStatus());
        }

        @Test
        @DisplayName("should throw when no Stripe customer exists")
        void should_throw_when_no_stripe_customer() {
            subscription.setStripeCustomerId(null);
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(subscription));

            SereniaException exception = assertThrows(
                    SereniaException.class,
                    () -> stripeService.createPortalSession(USER_ID)
            );

            assertEquals(400, exception.getHttpStatus());
            assertTrue(exception.getMessage().contains("No Stripe customer found"));
        }

        @Test
        @DisplayName("should throw when Stripe customer ID is empty")
        void should_throw_when_stripe_customer_id_empty() {
            subscription.setStripeCustomerId("");
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.of(subscription));

            SereniaException exception = assertThrows(
                    SereniaException.class,
                    () -> stripeService.createPortalSession(USER_ID)
            );

            assertEquals(400, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("getOrCreateStripeCustomer")
    class GetOrCreateStripeCustomer {

        @Test
        @DisplayName("should return existing customer ID when present")
        void should_return_existing_customer_id() {
            subscription.setStripeCustomerId(STRIPE_CUSTOMER_ID);

            String result = stripeService.getOrCreateStripeCustomer(user, subscription);

            assertEquals(STRIPE_CUSTOMER_ID, result);
            verify(subscriptionRepository, never()).persist(any(Subscription.class));
        }

        @Test
        @DisplayName("should not create customer when ID already exists")
        void should_not_create_customer_when_id_exists() {
            subscription.setStripeCustomerId(STRIPE_CUSTOMER_ID);

            stripeService.getOrCreateStripeCustomer(user, subscription);

            verifyNoMoreInteractions(subscriptionRepository);
        }
    }
}

