package com.lofo.serenia.service.subscription.webhook.handlers.invoice;

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
import com.stripe.model.Invoice;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoicePaymentFailedHandler Tests")
class InvoicePaymentFailedHandlerTest {

    private InvoicePaymentFailedHandler handler;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private StripeObjectMapper objectMapper;

    @Mock
    private Event event;

    @Mock
    private PanacheQuery<Subscription> query;

    private Subscription subscription;
    private Invoice invoice;

    private static final String CUSTOMER_ID = "cus_test123";

    @BeforeEach
    void setUp() {
        handler = new InvoicePaymentFailedHandler(subscriptionRepository, objectMapper);

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .build();

        Plan plan = Plan.builder()
                .id(UUID.randomUUID())
                .name(PlanType.PLUS)
                .build();

        subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .user(user)
                .plan(plan)
                .stripeCustomerId(CUSTOMER_ID)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        invoice = new Invoice();
        invoice.setCustomer(CUSTOMER_ID);
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        @DisplayName("should return INVOICE_PAYMENT_FAILED event type")
        void should_return_correct_event_type() {
            assertEquals(StripeEventType.INVOICE_PAYMENT_FAILED, handler.getEventType());
        }

        @Test
        @DisplayName("should update status to PAST_DUE")
        void should_update_status_to_past_due() {
            when(objectMapper.deserialize(event, Invoice.class)).thenReturn(invoice);
            when(subscriptionRepository.find("stripeCustomerId", CUSTOMER_ID)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.of(subscription));

            handler.handle(event);

            assertEquals(SubscriptionStatus.PAST_DUE, subscription.getStatus());
            verify(subscriptionRepository).persist(subscription);
        }

        @Test
        @DisplayName("should throw WebhookProcessingException when subscription not found")
        void should_throw_when_not_found() {
            when(objectMapper.deserialize(event, Invoice.class)).thenReturn(invoice);
            when(subscriptionRepository.find("stripeCustomerId", CUSTOMER_ID)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.empty());

            assertThrows(WebhookProcessingException.class, () -> handler.handle(event));
            verify(subscriptionRepository, never()).persist(any(Subscription.class));
        }
    }
}

