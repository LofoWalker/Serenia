package com.lofo.serenia.service.subscription.mapper;

import com.lofo.serenia.exception.exceptions.WebhookProcessingException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeObjectMapper Tests")
class StripeObjectMapperTest {

    private StripeObjectMapper objectMapper;

    @Mock
    private Event event;

    @Mock
    private EventDataObjectDeserializer deserializer;

    @BeforeEach
    void setUp() {
        objectMapper = new StripeObjectMapper();
    }

    @Nested
    @DisplayName("deserialize")
    class Deserialize {

        @Test
        @DisplayName("should deserialize when safe deserialization succeeds")
        void should_deserialize_safely() {
            Session session = new Session();
            session.setId("sess_test123");

            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));

            Session result = objectMapper.deserialize(event, Session.class);

            assertNotNull(result);
            assertEquals("sess_test123", result.getId());
        }

        @Test
        @DisplayName("should throw WebhookProcessingException on type mismatch in safe deserialization")
        void should_throw_on_type_mismatch_safe() {
            Invoice invoice = new Invoice();

            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(invoice));

            assertThrows(WebhookProcessingException.class,
                    () -> objectMapper.deserialize(event, Session.class));
        }

        @Test
        @DisplayName("should deserialize different Stripe object types")
        void should_deserialize_different_types() {
            Invoice invoice = new Invoice();
            invoice.setId("in_test123");

            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(invoice));

            Invoice result = objectMapper.deserialize(event, Invoice.class);

            assertNotNull(result);
            assertEquals("in_test123", result.getId());
        }
    }
}

