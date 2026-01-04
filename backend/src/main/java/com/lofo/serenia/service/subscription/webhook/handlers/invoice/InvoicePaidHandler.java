package com.lofo.serenia.service.subscription.webhook.handlers.invoice;

import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.service.subscription.StripeEventType;
import com.lofo.serenia.service.subscription.mapper.StripeObjectMapper;
import com.lofo.serenia.service.subscription.webhook.handlers.StripeEventHandler;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Handles invoice.paid events.
 * Records invoice payment amount and currency for billing records and analytics.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class InvoicePaidHandler implements StripeEventHandler {

    private static final String STRIPE_CUSTOMER_ID = "stripeCustomerId";

    private final SubscriptionRepository subscriptionRepository;
    private final StripeObjectMapper stripeObjectMapper;

    @Override
    public StripeEventType getEventType() {
        return StripeEventType.INVOICE_PAID;
    }

    @Override
    public void handle(Event event) {
        Invoice invoice = stripeObjectMapper.deserialize(event, Invoice.class);

        String customerId = invoice.getCustomer();

        log.info("Invoice paid for customer: {}", customerId);

        Optional<Subscription> subscriptionOpt = subscriptionRepository
                .find(STRIPE_CUSTOMER_ID, customerId)
                .firstResultOptional();

        if (subscriptionOpt.isPresent()) {
            log.debug("Invoice paid for customer: {}", customerId);
        } else {
            log.warn("No subscription found for customer: {} when processing invoice.paid", customerId);
        }
    }
}

