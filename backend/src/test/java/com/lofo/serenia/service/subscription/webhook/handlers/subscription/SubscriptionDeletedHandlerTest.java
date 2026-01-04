package com.lofo.serenia.service.subscription.webhook.handlers.subscription;

import com.lofo.serenia.exception.exceptions.WebhookProcessingException;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.subscription.SubscriptionStatus;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.service.subscription.StripeEventType;
import com.lofo.serenia.service.subscription.discount.DiscountProcessor;
import com.lofo.serenia.service.subscription.mapper.StripeObjectMapper;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.stripe.model.Event;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionDeletedHandler Tests")
class SubscriptionDeletedHandlerTest {

    private SubscriptionDeletedHandler handler;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private StripeObjectMapper objectMapper;

    @Mock
    private DiscountProcessor discountProcessor;

    @Mock
    private Event event;

    @Mock
    private PanacheQuery<Subscription> query;

    private Subscription subscription;
    private com.stripe.model.Subscription stripeSubscription;
    private Plan freePlan;

    private static final String CUSTOMER_ID = "cus_test123";

    @BeforeEach
    void setUp() {
        handler = new SubscriptionDeletedHandler(
                subscriptionRepository,
                planRepository,
                objectMapper,
                discountProcessor
        );

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .build();

        Plan plusPlan = Plan.builder()
                .id(UUID.randomUUID())
                .name(PlanType.PLUS)
                .build();

        freePlan = Plan.builder()
                .id(UUID.randomUUID())
                .name(PlanType.FREE)
                .build();

        subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(plusPlan)
                .stripeCustomerId(CUSTOMER_ID)
                .stripeSubscriptionId("sub_test123")
                .status(SubscriptionStatus.ACTIVE)
                .cancelAtPeriodEnd(true)
                .build();

        stripeSubscription = new com.stripe.model.Subscription();
        stripeSubscription.setCustomer(CUSTOMER_ID);
        stripeSubscription.setId("sub_test123");
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        @DisplayName("should return SUBSCRIPTION_DELETED event type")
        void should_return_correct_event_type() {
            assertEquals(StripeEventType.SUBSCRIPTION_DELETED, handler.getEventType());
        }

        @Test
        @DisplayName("should downgrade user to FREE plan and clear subscription data")
        void should_downgrade_to_free_plan() {
            when(objectMapper.deserialize(event, com.stripe.model.Subscription.class))
                    .thenReturn(stripeSubscription);
            when(subscriptionRepository.find("stripeCustomerId", CUSTOMER_ID)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.of(subscription));
            when(planRepository.getFreePlan()).thenReturn(freePlan);

            handler.handle(event);

            assertEquals(freePlan, subscription.getPlan());
            assertNull(subscription.getStripeSubscriptionId());
            assertNull(subscription.getStripeCustomerId());
            assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
            assertFalse(subscription.getCancelAtPeriodEnd());
            assertNull(subscription.getCurrentPeriodEnd());

            verify(discountProcessor).clearDiscount(subscription);
            verify(subscriptionRepository).persist(subscription);
        }

        @Test
        @DisplayName("should throw WebhookProcessingException when subscription not found")
        void should_throw_when_not_found() {
            when(objectMapper.deserialize(event, com.stripe.model.Subscription.class))
                    .thenReturn(stripeSubscription);
            when(subscriptionRepository.find("stripeCustomerId", CUSTOMER_ID)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.empty());

            assertThrows(WebhookProcessingException.class, () -> handler.handle(event));
            verify(planRepository, never()).getFreePlan();
        }
    }
}

