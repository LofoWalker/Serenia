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
        long amountPaid = invoice.getAmountPaid();
        String currency = invoice.getCurrency();

        log.info("Invoice paid for customer: {}, amount: {} {}", customerId, amountPaid / 100.0, currency);

        Optional<Subscription> subscriptionOpt = subscriptionRepository
                .find(STRIPE_CUSTOMER_ID, customerId)
                .firstResultOptional();

        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            subscription.setLastInvoiceAmount((int) amountPaid);
            subscription.setCurrency(currency);

            logInvoicePaidWithDiscount(subscription, amountPaid, currency);

            subscriptionRepository.persist(subscription);
            log.debug("Updated subscription with last invoice amount and currency");
        } else {
            log.warn("No subscription found for customer: {} when processing invoice.paid", customerId);
        }
    }

    /**
     * Logs invoice payment details, including discount information if applicable.
     */
    private void logInvoicePaidWithDiscount(Subscription subscription, long amountPaid, String currency) {
        if (subscription.getStripeCouponId() != null) {
            log.info("Invoice paid with active discount - coupon: {}, type: {}, value: {}, " +
                    "amountAfterDiscount: {} {}",
                    subscription.getStripeCouponId(),
                    subscription.getDiscountType(),
                    subscription.getDiscountValue(),
                    amountPaid / 100.0,
                    currency);
        }
    }
}

