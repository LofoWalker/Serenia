package com.lofo.serenia.service.subscription.webhook.handlers.checkout;

import com.lofo.serenia.service.subscription.StripeEventType;
import com.lofo.serenia.service.subscription.mapper.StripeObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CheckoutSessionExpiredHandler Tests")
class CheckoutSessionExpiredHandlerTest {

    private CheckoutSessionExpiredHandler handler;

    @Mock
    private StripeObjectMapper objectMapper;

    @Mock
    private Event event;

    private Session session;

    private static final String CUSTOMER_ID = "cus_test123";
    private static final String SESSION_ID = "sess_test123";

    @BeforeEach
    void setUp() {
        handler = new CheckoutSessionExpiredHandler(objectMapper);

        session = new Session();
        session.setCustomer(CUSTOMER_ID);
        session.setId(SESSION_ID);
    }

    @Nested
    @DisplayName("handle")
    class Handle {

        @Test
        @DisplayName("should return CHECKOUT_SESSION_EXPIRED event type")
        void should_return_correct_event_type() {
            assertEquals(StripeEventType.CHECKOUT_SESSION_EXPIRED, handler.getEventType());
        }

        @Test
        @DisplayName("should deserialize session and log warning")
        void should_deserialize_and_log() {
            when(objectMapper.deserialize(event, Session.class)).thenReturn(session);

            assertDoesNotThrow(() -> handler.handle(event));

            verify(objectMapper).deserialize(event, Session.class);
        }

        @Test
        @DisplayName("should handle session without throwing")
        void should_handle_without_throwing() {
            when(objectMapper.deserialize(event, Session.class)).thenReturn(session);

            handler.handle(event);

            // Verify no exception is thrown
        }
    }
}

