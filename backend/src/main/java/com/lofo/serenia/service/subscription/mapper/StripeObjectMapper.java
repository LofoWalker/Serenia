package com.lofo.serenia.service.subscription.mapper;

import com.lofo.serenia.exception.exceptions.WebhookProcessingException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

/**
 * Centralized Stripe object deserialization utility.
 * Handles safe and unsafe deserialization with proper error handling.
 */
@Slf4j
@ApplicationScoped
public class StripeObjectMapper {

    /**
     * Deserializes a Stripe event's data into the specified object type.
     * Attempts safe deserialization first, then unsafe as fallback.
     *
     * @param event the Stripe event containing serialized data
     * @param clazz the target class to deserialize into
     * @param <T> the type parameter extending StripeObject
     * @return the deserialized object
     * @throws WebhookProcessingException if deserialization fails or type mismatch occurs
     */
    @SuppressWarnings("unchecked")
    public <T extends StripeObject> T deserialize(Event event, Class<T> clazz) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        // Try safe deserialization first
        if (deserializer.getObject().isPresent()) {
            StripeObject obj = deserializer.getObject().get();
            if (clazz.isInstance(obj)) {
                return (T) obj;
            }
            log.error("Expected {} but got {}", clazz.getSimpleName(), obj.getClass().getSimpleName());
            throw new WebhookProcessingException(
                    String.format("Type mismatch: expected %s but got %s",
                            clazz.getSimpleName(),
                            obj.getClass().getSimpleName())
            );
        }

        try {
            StripeObject obj = deserializer.deserializeUnsafe();
            if (clazz.isInstance(obj)) {
                log.debug("Used unsafe deserialization for event: {}", event.getId());
                return (T) obj;
            }
            log.error("Expected {} but got {} (unsafe)", clazz.getSimpleName(), obj.getClass().getSimpleName());
            throw new WebhookProcessingException(
                    String.format("Type mismatch on unsafe deserialization: expected %s but got %s",
                            clazz.getSimpleName(),
                            obj.getClass().getSimpleName())
            );
        } catch (Exception e) {
            log.error("Failed to deserialize event data for event: {} - {}", event.getId(), e.getMessage());
            throw new WebhookProcessingException(
                    String.format("Failed to deserialize %s from event %s: %s",
                            clazz.getSimpleName(),
                            event.getId(),
                            e.getMessage()),
                    e
            );
        }
    }
}

