package com.lofo.serenia.service.subscription.webhook;

import com.lofo.serenia.service.subscription.StripeEventType;
import com.lofo.serenia.service.subscription.webhook.handlers.StripeEventHandler;
import com.stripe.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.enterprise.inject.Instance;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeWebhookService Dispatcher Tests")
class StripeWebhookDispatcherTest {

    private StripeWebhookService webhookService;

    @Mock
    private Instance<StripeEventHandler> handlersInstance;

    @Mock
    private StripeEventHandler checkoutHandler;

    @Mock
    private StripeEventHandler subscriptionHandler;

    @Mock
    private Event event;

    @BeforeEach
    void setUp() {
        webhookService = new StripeWebhookService(handlersInstance);
    }

    @Nested
    @DisplayName("handleEvent")
    class HandleEvent {

        @Test
        @DisplayName("should route event to appropriate handler")
        void should_route_event_to_handler() {
            when(checkoutHandler.getEventType()).thenReturn(StripeEventType.CHECKOUT_SESSION_COMPLETED);
            when(event.getType()).thenReturn("checkout.session.completed");

            List<StripeEventHandler> handlers = new ArrayList<>();
            handlers.add(checkoutHandler);

            when(handlersInstance.iterator()).thenReturn(handlers.iterator());

            webhookService.handleEvent(event);

            verify(checkoutHandler).handle(event);
        }

        @Test
        @DisplayName("should not route to wrong handler")
        void should_not_route_to_wrong_handler() {
            when(checkoutHandler.getEventType()).thenReturn(StripeEventType.CHECKOUT_SESSION_COMPLETED);
            when(subscriptionHandler.getEventType()).thenReturn(StripeEventType.SUBSCRIPTION_CREATED);
            when(event.getType()).thenReturn("customer.subscription.created");

            List<StripeEventHandler> handlers = new ArrayList<>();
            handlers.add(checkoutHandler);
            handlers.add(subscriptionHandler);

            when(handlersInstance.iterator()).thenReturn(handlers.iterator());

            webhookService.handleEvent(event);

            verify(checkoutHandler, never()).handle(event);
            verify(subscriptionHandler).handle(event);
        }

        @Test
        @DisplayName("should handle unknown event type gracefully")
        void should_handle_unknown_event_type() {
            when(event.getType()).thenReturn("unknown.event");

            assertDoesNotThrow(() -> webhookService.handleEvent(event));
        }

        @Test
        @DisplayName("should handle multiple events sequentially")
        void should_handle_multiple_events() {
            when(checkoutHandler.getEventType()).thenReturn(StripeEventType.CHECKOUT_SESSION_COMPLETED);
            when(subscriptionHandler.getEventType()).thenReturn(StripeEventType.SUBSCRIPTION_CREATED);

            Event event1 = new Event();
            event1.setType("checkout.session.completed");

            Event event2 = new Event();
            event2.setType("customer.subscription.created");

            List<StripeEventHandler> handlers = new ArrayList<>();
            handlers.add(checkoutHandler);
            handlers.add(subscriptionHandler);

            when(handlersInstance.iterator()).thenReturn(handlers.iterator());

            webhookService.handleEvent(event1);
            webhookService.handleEvent(event2);

            verify(checkoutHandler).handle(event1);
            verify(subscriptionHandler).handle(event2);
        }

        @Test
        @DisplayName("should propagate exceptions from handlers")
        void should_propagate_handler_exceptions() {
            when(checkoutHandler.getEventType()).thenReturn(StripeEventType.CHECKOUT_SESSION_COMPLETED);
            when(event.getType()).thenReturn("checkout.session.completed");
            doThrow(new RuntimeException("Handler error")).when(checkoutHandler).handle(event);

            List<StripeEventHandler> handlers = new ArrayList<>();
            handlers.add(checkoutHandler);

            when(handlersInstance.iterator()).thenReturn(handlers.iterator());

            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> webhookService.handleEvent(event));
        }
    }
}

