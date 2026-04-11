package com.lofo.serenia.service.subscription.mapper;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;

/**
 * Utility for converting between different date/time formats used in Stripe integration.
 */
@ApplicationScoped
public class DateTimeConverter {

    /**
     * Converts a Unix epoch timestamp (seconds) to an {@link Instant}.
     *
     * @param epochSecond the Unix epoch timestamp in seconds
     * @return the corresponding Instant, or null if input is null
     */
    public Instant convertEpochToDateTime(Long epochSecond) {
        if (epochSecond == null) {
            return null;
        }
        return Instant.ofEpochSecond(epochSecond);
    }
}

