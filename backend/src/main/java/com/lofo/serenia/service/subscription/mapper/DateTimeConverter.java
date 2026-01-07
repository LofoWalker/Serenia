package com.lofo.serenia.service.subscription.mapper;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Utility for converting between different date/time formats used in Stripe integration.
 * Centralizes time zone handling and epoch conversions.
 */
@ApplicationScoped
public class DateTimeConverter {

    /**
     * Converts a Unix epoch timestamp (seconds) to LocalDateTime using system default timezone.
     *
     * @param epochSecond the Unix epoch timestamp in seconds
     * @return the corresponding LocalDateTime
     */
    public LocalDateTime convertEpochToDateTime(Long epochSecond) {
        if (epochSecond == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(epochSecond),
                ZoneId.systemDefault()
        );
    }
}

