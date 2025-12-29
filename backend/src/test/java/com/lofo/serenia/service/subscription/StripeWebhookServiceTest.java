package com.lofo.serenia.service.subscription;

import com.lofo.serenia.exception.exceptions.WebhookProcessingException;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.subscription.SubscriptionStatus;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import com.stripe.model.Price;
import com.stripe.model.checkout.Session;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeWebhookService Tests")
class StripeWebhookServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private Event event;

    @Mock
    private EventDataObjectDeserializer deserializer;

    private StripeWebhookService webhookService;

    private static final String CUSTOMER_ID = "cus_test123";
    private static final String SUBSCRIPTION_ID = "sub_test123";
    private static final String PRICE_ID = "price_test123";

    private Subscription subscription;
    private Plan freePlan;
    private Plan plusPlan;

    @BeforeEach
    void setUp() {
        webhookService = new StripeWebhookService(subscriptionRepository, planRepository);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .build();

        freePlan = Plan.builder()
                .id(UUID.randomUUID())
                .name(PlanType.FREE)
                .build();

        plusPlan = Plan.builder()
                .id(UUID.randomUUID())
                .name(PlanType.PLUS)
                .stripePriceId(PRICE_ID)
                .build();

        subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(freePlan)
                .stripeCustomerId(CUSTOMER_ID)
                .status(SubscriptionStatus.ACTIVE)
                .monthlyPeriodStart(LocalDateTime.now())
                .dailyPeriodStart(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("handleEvent - routing")
    class HandleEventRouting {

        @Test
        @DisplayName("should throw WebhookProcessingException when checkout.session.completed deserialization fails")
        void should_throw_when_checkout_session_deserialization_fails() {
            when(event.getType()).thenReturn("checkout.session.completed");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.empty());

            assertThrows(WebhookProcessingException.class, () -> webhookService.handleEvent(event));
        }

        @Test
        @DisplayName("should throw WebhookProcessingException when customer.subscription.created deserialization fails")
        void should_throw_when_subscription_created_deserialization_fails() {
            when(event.getType()).thenReturn("customer.subscription.created");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.empty());

            assertThrows(WebhookProcessingException.class, () -> webhookService.handleEvent(event));
        }

        @Test
        @DisplayName("should throw WebhookProcessingException when customer.subscription.updated deserialization fails")
        void should_throw_when_subscription_updated_deserialization_fails() {
            when(event.getType()).thenReturn("customer.subscription.updated");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.empty());

            assertThrows(WebhookProcessingException.class, () -> webhookService.handleEvent(event));
        }

        @Test
        @DisplayName("should throw WebhookProcessingException when customer.subscription.deleted deserialization fails")
        void should_throw_when_subscription_deleted_deserialization_fails() {
            when(event.getType()).thenReturn("customer.subscription.deleted");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.empty());

            assertThrows(WebhookProcessingException.class, () -> webhookService.handleEvent(event));
        }

        @Test
        @DisplayName("should throw WebhookProcessingException when invoice.payment_succeeded deserialization fails")
        void should_throw_when_payment_succeeded_deserialization_fails() {
            when(event.getType()).thenReturn("invoice.payment_succeeded");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.empty());

            assertThrows(WebhookProcessingException.class, () -> webhookService.handleEvent(event));
        }

        @Test
        @DisplayName("should throw WebhookProcessingException when invoice.payment_failed deserialization fails")
        void should_throw_when_payment_failed_deserialization_fails() {
            when(event.getType()).thenReturn("invoice.payment_failed");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.empty());

            assertThrows(WebhookProcessingException.class, () -> webhookService.handleEvent(event));
        }

        @Test
        @DisplayName("should ignore unhandled event types")
        void should_ignore_unhandled_events() {
            when(event.getType()).thenReturn("some.other.event");

            assertDoesNotThrow(() -> webhookService.handleEvent(event));
            verify(subscriptionRepository, never()).persist(any(Subscription.class));
        }
    }

    @Nested
    @DisplayName("handleCheckoutSessionCompleted")
    class HandleCheckoutSessionCompleted {

        @Mock
        private Session session;

        @Test
        @DisplayName("should set subscription ID and status when checkout completed")
        void should_set_subscription_id_when_checkout_completed() {
            subscription.setStripeSubscriptionId(null);

            when(event.getType()).thenReturn("checkout.session.completed");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));
            when(session.getCustomer()).thenReturn(CUSTOMER_ID);
            when(session.getSubscription()).thenReturn(SUBSCRIPTION_ID);

            mockSubscriptionRepositoryFind();

            webhookService.handleEvent(event);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).persist(captor.capture());

            Subscription saved = captor.getValue();
            assertEquals(SUBSCRIPTION_ID, saved.getStripeSubscriptionId());
            assertEquals(SubscriptionStatus.ACTIVE, saved.getStatus());
        }

        @Test
        @DisplayName("should not update if subscription ID already set")
        void should_not_update_if_subscription_id_already_set() {
            subscription.setStripeSubscriptionId(SUBSCRIPTION_ID);

            when(event.getType()).thenReturn("checkout.session.completed");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));
            when(session.getCustomer()).thenReturn(CUSTOMER_ID);
            when(session.getSubscription()).thenReturn(SUBSCRIPTION_ID);

            mockSubscriptionRepositoryFind();

            webhookService.handleEvent(event);

            verify(subscriptionRepository, never()).persist(any(Subscription.class));
        }

        @Test
        @DisplayName("should throw exception when no subscription found for customer")
        void should_throw_when_no_subscription_found() {
            when(event.getType()).thenReturn("checkout.session.completed");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));
            when(session.getCustomer()).thenReturn("unknown_customer");

            mockSubscriptionRepositoryFindEmpty();

            assertThrows(WebhookProcessingException.class, () -> webhookService.handleEvent(event));
            verify(subscriptionRepository, never()).persist(any(Subscription.class));
        }
    }

    @Nested
    @DisplayName("handleSubscriptionDeleted")
    class HandleSubscriptionDeleted {

        @Mock
        private com.stripe.model.Subscription stripeSubscription;

        @Test
        @DisplayName("should return to FREE plan when subscription deleted")
        void should_return_to_free_plan() {
            subscription.setPlan(plusPlan);
            subscription.setStripeSubscriptionId(SUBSCRIPTION_ID);

            when(event.getType()).thenReturn("customer.subscription.deleted");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(stripeSubscription));
            when(stripeSubscription.getCustomer()).thenReturn(CUSTOMER_ID);

            mockSubscriptionRepositoryFind();
            when(planRepository.getFreePlan()).thenReturn(freePlan);

            webhookService.handleEvent(event);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).persist(captor.capture());

            Subscription saved = captor.getValue();
            assertEquals(freePlan, saved.getPlan());
            assertNull(saved.getStripeSubscriptionId());
            assertEquals(SubscriptionStatus.ACTIVE, saved.getStatus());
            assertFalse(saved.getCancelAtPeriodEnd());
            assertNull(saved.getCurrentPeriodEnd());
        }
    }

    @Nested
    @DisplayName("handleInvoicePaymentFailed")
    class HandleInvoicePaymentFailed {

        @Mock
        private Invoice invoice;

        @Test
        @DisplayName("should set status to PAST_DUE when payment fails")
        void should_set_status_to_past_due() {
            when(event.getType()).thenReturn("invoice.payment_failed");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(invoice));
            when(invoice.getCustomer()).thenReturn(CUSTOMER_ID);

            mockSubscriptionRepositoryFind();

            webhookService.handleEvent(event);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).persist(captor.capture());

            assertEquals(SubscriptionStatus.PAST_DUE, captor.getValue().getStatus());
        }
    }

    @Nested
    @DisplayName("handleInvoicePaymentSucceeded")
    class HandleInvoicePaymentSucceeded {

        @Mock
        private Invoice invoice;

        @Test
        @DisplayName("should set status to ACTIVE when payment succeeds and was PAST_DUE")
        void should_set_status_to_active_when_was_past_due() {
            subscription.setStatus(SubscriptionStatus.PAST_DUE);

            when(event.getType()).thenReturn("invoice.payment_succeeded");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(invoice));
            when(invoice.getCustomer()).thenReturn(CUSTOMER_ID);
            when(invoice.getSubscription()).thenReturn(SUBSCRIPTION_ID);

            mockSubscriptionRepositoryFind();

            webhookService.handleEvent(event);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).persist(captor.capture());

            assertEquals(SubscriptionStatus.ACTIVE, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("should not update when status is already ACTIVE")
        void should_not_update_when_already_active() {
            subscription.setStatus(SubscriptionStatus.ACTIVE);

            when(event.getType()).thenReturn("invoice.payment_succeeded");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(invoice));
            when(invoice.getCustomer()).thenReturn(CUSTOMER_ID);
            when(invoice.getSubscription()).thenReturn(SUBSCRIPTION_ID);

            mockSubscriptionRepositoryFind();

            webhookService.handleEvent(event);

            verify(subscriptionRepository, never()).persist(any(Subscription.class));
        }
    }

    @Nested
    @DisplayName("handleSubscriptionUpdated")
    class HandleSubscriptionUpdated {

        @Mock
        private com.stripe.model.Subscription stripeSubscription;

        @Mock
        private SubscriptionItemCollection items;

        @Mock
        private SubscriptionItem item;

        @Mock
        private Price price;

        @Test
        @DisplayName("should update plan based on price ID")
        void should_update_plan_based_on_price_id() {
            subscription.setPlan(freePlan);

            when(event.getType()).thenReturn("customer.subscription.updated");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(stripeSubscription));
            when(stripeSubscription.getCustomer()).thenReturn(CUSTOMER_ID);
            when(stripeSubscription.getId()).thenReturn(SUBSCRIPTION_ID);
            when(stripeSubscription.getStatus()).thenReturn("active");
            when(stripeSubscription.getCancelAtPeriodEnd()).thenReturn(false);
            when(stripeSubscription.getCurrentPeriodEnd()).thenReturn(null);
            when(stripeSubscription.getItems()).thenReturn(items);
            when(items.getData()).thenReturn(List.of(item));
            when(item.getPrice()).thenReturn(price);
            when(price.getId()).thenReturn(PRICE_ID);

            mockSubscriptionRepositoryFind();
            mockPlanRepositoryFindByPriceId();

            webhookService.handleEvent(event);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).persist(captor.capture());

            assertEquals(plusPlan, captor.getValue().getPlan());
        }

        @Test
        @DisplayName("should update cancel_at_period_end flag")
        void should_update_cancel_at_period_end() {
            when(event.getType()).thenReturn("customer.subscription.updated");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(stripeSubscription));
            when(stripeSubscription.getCustomer()).thenReturn(CUSTOMER_ID);
            when(stripeSubscription.getId()).thenReturn(SUBSCRIPTION_ID);
            when(stripeSubscription.getStatus()).thenReturn("active");
            when(stripeSubscription.getCancelAtPeriodEnd()).thenReturn(true);
            when(stripeSubscription.getCurrentPeriodEnd()).thenReturn(null);
            when(stripeSubscription.getItems()).thenReturn(null);

            mockSubscriptionRepositoryFind();

            webhookService.handleEvent(event);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).persist(captor.capture());

            assertTrue(captor.getValue().getCancelAtPeriodEnd());
        }

        @Test
        @DisplayName("should map Stripe status correctly")
        void should_map_stripe_status_correctly() {
            when(event.getType()).thenReturn("customer.subscription.updated");
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(stripeSubscription));
            when(stripeSubscription.getCustomer()).thenReturn(CUSTOMER_ID);
            when(stripeSubscription.getId()).thenReturn(SUBSCRIPTION_ID);
            when(stripeSubscription.getStatus()).thenReturn("past_due");
            when(stripeSubscription.getCancelAtPeriodEnd()).thenReturn(false);
            when(stripeSubscription.getCurrentPeriodEnd()).thenReturn(null);
            when(stripeSubscription.getItems()).thenReturn(null);

            mockSubscriptionRepositoryFind();

            webhookService.handleEvent(event);

            ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
            verify(subscriptionRepository).persist(captor.capture());

            assertEquals(SubscriptionStatus.PAST_DUE, captor.getValue().getStatus());
        }
    }

    // Helper methods

    @SuppressWarnings("unchecked")
    private void mockSubscriptionRepositoryFind() {
        PanacheQuery<Subscription> query = mock(PanacheQuery.class);
        when(subscriptionRepository.find("stripeCustomerId", CUSTOMER_ID)).thenReturn(query);
        when(query.firstResultOptional()).thenReturn(Optional.of(subscription));
    }

    @SuppressWarnings("unchecked")
    private void mockSubscriptionRepositoryFindEmpty() {
        PanacheQuery<Subscription> query = mock(PanacheQuery.class);
        when(subscriptionRepository.find(anyString(), anyString())).thenReturn(query);
        when(query.firstResultOptional()).thenReturn(Optional.empty());
    }

    @SuppressWarnings("unchecked")
    private void mockPlanRepositoryFindByPriceId() {
        PanacheQuery<Plan> query = mock(PanacheQuery.class);
        when(planRepository.find("stripePriceId", PRICE_ID)).thenReturn(query);
        when(query.firstResultOptional()).thenReturn(Optional.of(plusPlan));
    }
}

