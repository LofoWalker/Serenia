package com.lofo.serenia.service.subscription.webhook.handlers.checkout;

import com.lofo.serenia.exception.exceptions.WebhookProcessingException;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.subscription.SubscriptionStatus;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.service.subscription.StripeEventType;
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
import com.stripe.model.checkout.Session;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckoutSessionCompletedHandler Tests")
class CheckoutSessionCompletedHandlerTest {

    private CheckoutSessionCompletedHandler handler;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private StripeObjectMapper objectMapper;

    @Mock
    private Event event;

    @Mock
    private PanacheQuery<Subscription> query;

    private Subscription subscription;
    private Session session;

    private static final String CUSTOMER_ID = "cus_test123";
    private static final String SUBSCRIPTION_ID = "sub_test123";

    @BeforeEach
    void setUp() {
        handler = new CheckoutSessionCompletedHandler(subscriptionRepository, objectMapper);

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

        session = new Session();
        session.setCustomer(CUSTOMER_ID);
        session.setSubscription(SUBSCRIPTION_ID);
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        @DisplayName("should return CHECKOUT_SESSION_COMPLETED event type")
        void should_return_correct_event_type() {
            assertEquals(StripeEventType.CHECKOUT_SESSION_COMPLETED, handler.getEventType());
        }

        @Test
        @DisplayName("should update subscription with Stripe subscription ID")
        void should_update_subscription_id() {
            when(objectMapper.deserialize(event, Session.class)).thenReturn(session);
            when(subscriptionRepository.find("stripeCustomerId", CUSTOMER_ID)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.of(subscription));

            handler.handle(event);

            assertEquals(SUBSCRIPTION_ID, subscription.getStripeSubscriptionId());
            assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
            verify(subscriptionRepository).persist(subscription);
        }

        @Test
        @DisplayName("should not update subscription ID if already set")
        void should_not_update_if_already_set() {
            subscription.setStripeSubscriptionId("sub_existing");
            when(objectMapper.deserialize(event, Session.class)).thenReturn(session);
            when(subscriptionRepository.find("stripeCustomerId", CUSTOMER_ID)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.of(subscription));

            handler.handle(event);

            assertEquals("sub_existing", subscription.getStripeSubscriptionId());
            verify(subscriptionRepository, never()).persist(subscription);
        }

        @Test
        @DisplayName("should throw WebhookProcessingException when subscription not found")
        void should_throw_when_subscription_not_found() {
            when(objectMapper.deserialize(event, Session.class)).thenReturn(session);
            when(subscriptionRepository.find("stripeCustomerId", CUSTOMER_ID)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.empty());

            assertThrows(WebhookProcessingException.class, () -> handler.handle(event));
            verify(subscriptionRepository, never()).persist(any(Subscription.class));
        }

        @Test
        @DisplayName("should throw when deserialization fails")
        void should_throw_when_deserialization_fails() {
            when(objectMapper.deserialize(event, Session.class))
                    .thenThrow(new WebhookProcessingException("Deserialization failed"));

            assertThrows(WebhookProcessingException.class, () -> handler.handle(event));
        }
    }
}

