package com.lofo.serenia.service.subscription.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
        @DisplayName("should convert valid epoch timestamp to LocalDateTime")
        void should_convert_valid_epoch() {
            long epochSecond = 1672531200L; // 2023-01-01 00:00:00 UTC

            LocalDateTime result = dateTimeConverter.convertEpochToDateTime(epochSecond);

            assertNotNull(result);
            assertEquals(1, result.getDayOfMonth());
            assertEquals(1, result.getMonthValue());
            assertEquals(2023, result.getYear());
        }

        @Test
        @DisplayName("should return null for null epoch")
        void should_return_null_for_null_epoch() {
            LocalDateTime result = dateTimeConverter.convertEpochToDateTime(null);

            assertNull(result);
        }

        @Test
        @DisplayName("should use system default timezone")
        void should_use_system_timezone() {
            long epochSecond = 0L; // 1970-01-01 00:00:00 UTC

            LocalDateTime result = dateTimeConverter.convertEpochToDateTime(epochSecond);

            assertNotNull(result);
            // Verify it's in system timezone
            LocalDateTime expected = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(epochSecond),
                    ZoneId.systemDefault()
            );
            assertEquals(expected, result);
        }

        @Test
        @DisplayName("should handle large epoch values")
        void should_handle_large_epoch_values() {
            long epochSecond = 2147483647L; // Max 32-bit int (2038-01-19)

            LocalDateTime result = dateTimeConverter.convertEpochToDateTime(epochSecond);

            assertNotNull(result);
            assertEquals(2038, result.getYear());
        }
    }
}

