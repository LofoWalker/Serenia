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
            validateType(obj, clazz, false);
            return (T) obj;
        }

        // Fallback to unsafe deserialization
        try {
            StripeObject obj = deserializer.deserializeUnsafe();
            validateType(obj, clazz, true);
            return (T) obj;
        } catch (Exception e) {
            throw handleDeserializationError(event, clazz, e);
        }
    }

    /**
     * Validates that the deserialized object matches the expected type.
     *
     * @param obj the deserialized Stripe object
     * @param expectedClass the expected class type
     * @param isUnsafe whether this was from unsafe deserialization
     * @throws WebhookProcessingException if type mismatch
     */
    private <T extends StripeObject> void validateType(StripeObject obj, Class<T> expectedClass, boolean isUnsafe) {
        if (!expectedClass.isInstance(obj)) {
            String suffix = isUnsafe ? " (unsafe)" : "";
            String message = String.format("Type mismatch: expected %s but got %s%s",
                    expectedClass.getSimpleName(),
                    obj.getClass().getSimpleName(),
                    suffix);
            log.error(message);
            throw new WebhookProcessingException(message);
        }
    }

    /**
     * Handles deserialization errors with proper logging.
     *
     * @param event the event being deserialized
     * @param targetClass the target class
     * @param cause the underlying exception
     * @return a WebhookProcessingException with context
     */
    private <T extends StripeObject> WebhookProcessingException handleDeserializationError(
            Event event,
            Class<T> targetClass,
            Exception cause) {
        String message = String.format("Failed to deserialize %s from event %s: %s",
                targetClass.getSimpleName(),
                event.getId(),
                cause.getMessage());
        log.error(message, cause);
        return new WebhookProcessingException(message, cause);
    }
}


