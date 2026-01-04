package com.lofo.serenia.service.subscription.webhook.handlers.checkout;

import com.lofo.serenia.service.subscription.StripeEventType;
import com.lofo.serenia.service.subscription.mapper.StripeObjectMapper;
import com.lofo.serenia.service.subscription.webhook.handlers.StripeEventHandler;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles checkout.session.expired events.
 * Logs when a checkout session expires without completion.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class CheckoutSessionExpiredHandler implements StripeEventHandler {

    private final StripeObjectMapper stripeObjectMapper;

    @Override
    public StripeEventType getEventType() {
        return StripeEventType.CHECKOUT_SESSION_EXPIRED;
    }

    @Override
    public void handle(Event event) {
        Session session = stripeObjectMapper.deserialize(event, Session.class);

        String customerId = session.getCustomer();
        String sessionId = session.getId();

        log.warn("Checkout session expired for customer: {}, sessionId: {}", customerId, sessionId);
    }
}

