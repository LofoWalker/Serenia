package com.lofo.serenia.service.subscription.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DateTimeConverter Tests")
class DateTimeConverterTest {

    private DateTimeConverter dateTimeConverter;

    @BeforeEach
    void setUp() {
        dateTimeConverter = new DateTimeConverter();
    }

    @Nested
    @DisplayName("convertEpochToDateTime")
    class ConvertEpochToDateTime {

        @Test
        @DisplayName("should convert valid epoch timestamp to Instant")
        void should_convert_valid_epoch() {
            long epochSecond = 1672531200L; // 2023-01-01 00:00:00 UTC

            Instant result = dateTimeConverter.convertEpochToDateTime(epochSecond);

            assertNotNull(result);
            assertEquals(Instant.ofEpochSecond(epochSecond), result);
        }

        @Test
        @DisplayName("should return null for null epoch")
        void should_return_null_for_null_epoch() {
            Instant result = dateTimeConverter.convertEpochToDateTime(null);

            assertNull(result);
        }

        @Test
        @DisplayName("should convert epoch 0 to Unix epoch origin")
        void should_convert_epoch_zero() {
            long epochSecond = 0L;

            Instant result = dateTimeConverter.convertEpochToDateTime(epochSecond);

            assertNotNull(result);
            assertEquals(Instant.EPOCH, result);
        }

        @Test
        @DisplayName("should handle large epoch values")
        void should_handle_large_epoch_values() {
            long epochSecond = 2147483647L; // Max 32-bit int (2038-01-19)

            Instant result = dateTimeConverter.convertEpochToDateTime(epochSecond);

            assertNotNull(result);
            assertEquals(Instant.ofEpochSecond(epochSecond), result);
        }
    }
}

