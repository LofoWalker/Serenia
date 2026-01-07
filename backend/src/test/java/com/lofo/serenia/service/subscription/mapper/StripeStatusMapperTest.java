package com.lofo.serenia.service.subscription.mapper;

import com.lofo.serenia.persistence.entity.subscription.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StripeStatusMapper Tests")
class StripeStatusMapperTest {

    private StripeStatusMapper statusMapper;

    @BeforeEach
    void setUp() {
        statusMapper = new StripeStatusMapper();
    }

    @Nested
    @DisplayName("mapStatus")
    class MapStatus {

        @Test
        @DisplayName("should map 'active' to ACTIVE")
        void should_map_active() {
            SubscriptionStatus result = statusMapper.mapStatus("active");
            assertEquals(SubscriptionStatus.ACTIVE, result);
        }

        @Test
        @DisplayName("should map 'past_due' to PAST_DUE")
        void should_map_past_due() {
            SubscriptionStatus result = statusMapper.mapStatus("past_due");
            assertEquals(SubscriptionStatus.PAST_DUE, result);
        }

        @Test
        @DisplayName("should map 'canceled' to CANCELED")
        void should_map_canceled() {
            SubscriptionStatus result = statusMapper.mapStatus("canceled");
            assertEquals(SubscriptionStatus.CANCELED, result);
        }

        @Test
        @DisplayName("should map 'incomplete' to INCOMPLETE")
        void should_map_incomplete() {
            SubscriptionStatus result = statusMapper.mapStatus("incomplete");
            assertEquals(SubscriptionStatus.INCOMPLETE, result);
        }

        @Test
        @DisplayName("should map 'incomplete_expired' to UNPAID")
        void should_map_incomplete_expired() {
            SubscriptionStatus result = statusMapper.mapStatus("incomplete_expired");
            assertEquals(SubscriptionStatus.UNPAID, result);
        }

        @Test
        @DisplayName("should map 'unpaid' to UNPAID")
        void should_map_unpaid() {
            SubscriptionStatus result = statusMapper.mapStatus("unpaid");
            assertEquals(SubscriptionStatus.UNPAID, result);
        }

        @Test
        @DisplayName("should map 'trialing' to ACTIVE")
        void should_map_trialing() {
            SubscriptionStatus result = statusMapper.mapStatus("trialing");
            assertEquals(SubscriptionStatus.ACTIVE, result);
        }

        @Test
        @DisplayName("should default to ACTIVE for unknown status")
        void should_default_to_active_for_unknown_status() {
            SubscriptionStatus result = statusMapper.mapStatus("unknown_status");
            assertEquals(SubscriptionStatus.ACTIVE, result);
        }

        @Test
        @DisplayName("should handle null gracefully")
        void should_handle_null() {
            assertThrows(NullPointerException.class, () -> statusMapper.mapStatus(null));
        }
    }
}

