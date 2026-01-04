package com.lofo.serenia.service.subscription.webhook.handlers.invoice;

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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvoicePaidHandler Tests")
class InvoicePaidHandlerTest {

    private InvoicePaidHandler handler;

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
    private static final long AMOUNT_PAID = 9999;
    private static final String CURRENCY = "EUR";

    @BeforeEach
    void setUp() {
        handler = new InvoicePaidHandler(subscriptionRepository, objectMapper);

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
        invoice.setAmountPaid(AMOUNT_PAID);
        invoice.setCurrency(CURRENCY);
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        @DisplayName("should return INVOICE_PAID event type")
        void should_return_correct_event_type() {
            assertEquals(StripeEventType.INVOICE_PAID, handler.getEventType());
        }

        @Test
        @DisplayName("should log invoice paid when subscription exists")
        void should_log_invoice_paid_when_subscription_found() {
            when(objectMapper.deserialize(event, Invoice.class)).thenReturn(invoice);
            when(subscriptionRepository.find("stripeCustomerId", CUSTOMER_ID)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.of(subscription));

            handler.handle(event);

            verify(subscriptionRepository).find("stripeCustomerId", CUSTOMER_ID);
            verify(subscriptionRepository, never()).persist(any(Subscription.class));
        }

        @Test
        @DisplayName("should handle missing subscription gracefully")
        void should_handle_missing_subscription() {
            when(objectMapper.deserialize(event, Invoice.class)).thenReturn(invoice);
            when(subscriptionRepository.find("stripeCustomerId", CUSTOMER_ID)).thenReturn(query);
            when(query.firstResultOptional()).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> handler.handle(event));
            verify(subscriptionRepository, never()).persist(any(Subscription.class));
        }
    }
}

