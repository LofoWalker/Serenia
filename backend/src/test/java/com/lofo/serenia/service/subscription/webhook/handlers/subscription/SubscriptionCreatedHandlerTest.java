package com.lofo.serenia.service.subscription.webhook.handlers.subscription;

import com.lofo.serenia.exception.exceptions.WebhookProcessingException;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.subscription.SubscriptionStatus;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.service.subscription.StripeEventType;
import com.lofo.serenia.service.subscription.mapper.StripeObjectMapper;
import com.lofo.serenia.service.subscription.orchestration.SubscriptionOrchestrator;
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
@DisplayName("SubscriptionCreatedHandler Tests")
class SubscriptionCreatedHandlerTest {

    private SubscriptionCreatedHandler handler;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private StripeObjectMapper objectMapper;

    @Mock
    private SubscriptionOrchestrator orchestrator;

    @Mock
    private Event event;

    @Mock
    private PanacheQuery<Subscription> query;

    private Subscription subscription;
    private com.stripe.model.Subscription stripeSubscription;

    private static final String CUSTOMER_ID = "cus_test123";

    @BeforeEach
    void setUp() {
        handler = new SubscriptionCreatedHandler(subscriptionRepository, objectMapper, orchestrator);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .build();

        Plan freePlan = Plan.builder()
                .id(UUID.randomUUID())
                .name(PlanType.FREE)
                .build();

        subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(freePlan)
                .stripeCustomerId(CUSTOMER_ID)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        stripeSubscription = new com.stripe.model.Subscription();
        stripeSubscription.setId("sub_test123");
        stripeSubscription.setCustomer(CUSTOMER_ID);
        stripeSubscription.setStatus("active");
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        @DisplayName("should return SUBSCRIPTION_CREATED event type")
        void should_return_correct_event_type() {
            assertEquals(StripeEventType.SUBSCRIPTION_CREATED, handler.getEventType());
        }

        @Test
        @DisplayName("should deserialize and synchronize subscription")
        void should_deserialize_and_synchronize() {
            when(objectMapper.deserialize(event, com.stripe.model.Subscription.class))
                    .thenReturn(stripeSubscription);
            when(subscriptionRepository.find("stripeCustomerId", CUSTOMER_ID)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.of(subscription));

            handler.handle(event);

            verify(objectMapper).deserialize(event, com.stripe.model.Subscription.class);
            verify(orchestrator).synchronizeFromStripe(subscription, stripeSubscription);
        }

        @Test
        @DisplayName("should throw WebhookProcessingException when subscription not found")
        void should_throw_when_subscription_not_found() {
            when(objectMapper.deserialize(event, com.stripe.model.Subscription.class))
                    .thenReturn(stripeSubscription);
            when(subscriptionRepository.find("stripeCustomerId", CUSTOMER_ID)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.empty());

            assertThrows(WebhookProcessingException.class, () -> handler.handle(event));
            verify(orchestrator, never()).synchronizeFromStripe(any(), any());
        }

        @Test
        @DisplayName("should throw when deserialization fails")
        void should_throw_when_deserialization_fails() {
            when(objectMapper.deserialize(event, com.stripe.model.Subscription.class))
                    .thenThrow(new WebhookProcessingException("Deserialization failed"));

            assertThrows(WebhookProcessingException.class, () -> handler.handle(event));
        }
    }
}

