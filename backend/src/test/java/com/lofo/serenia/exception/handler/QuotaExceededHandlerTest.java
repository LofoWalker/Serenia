package com.lofo.serenia.exception.handler;

import com.lofo.serenia.exception.exceptions.QuotaExceededException;
import com.lofo.serenia.rest.dto.out.QuotaErrorDTO;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuotaExceededHandler Unit Tests")
class QuotaExceededHandlerTest {

    private QuotaExceededHandler handler;

    @BeforeEach
    void setUp() {
        handler = new QuotaExceededHandler();
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("should return 429 status")
        void should_return_429_status() {
            QuotaExceededException exception = QuotaExceededException.monthlyTokenLimit(10000, 10000, 100);

            Response response = handler.toResponse(exception);

            assertThat(response.getStatus()).isEqualTo(429);
        }

        @Test
        @DisplayName("should return quota error dto with token type")
        void should_return_quota_error_dto_with_token_type() {
            QuotaExceededException exception = QuotaExceededException.monthlyTokenLimit(10000, 9500, 600);

            Response response = handler.toResponse(exception);
            QuotaErrorDTO errorDTO = (QuotaErrorDTO) response.getEntity();

            assertThat(errorDTO.quotaType()).isEqualTo("monthly_token_limit");
            assertThat(errorDTO.limit()).isEqualTo(10000);
            assertThat(errorDTO.current()).isEqualTo(9500);
            assertThat(errorDTO.requested()).isEqualTo(600);
        }

        @Test
        @DisplayName("should return quota error dto with message type")
        void should_return_quota_error_dto_with_message_type() {
            QuotaExceededException exception = QuotaExceededException.dailyMessageLimit(10, 10);

            Response response = handler.toResponse(exception);
            QuotaErrorDTO errorDTO = (QuotaErrorDTO) response.getEntity();

            assertThat(errorDTO.quotaType()).isEqualTo("daily_message_limit");
            assertThat(errorDTO.limit()).isEqualTo(10);
            assertThat(errorDTO.current()).isEqualTo(10);
        }

        @Test
        @DisplayName("should include all quota details")
        void should_include_all_quota_details() {
            int limit = 5000;
            int current = 4800;
            int requested = 300;
            QuotaExceededException exception = QuotaExceededException.monthlyTokenLimit(limit, current, requested);

            Response response = handler.toResponse(exception);
            QuotaErrorDTO errorDTO = (QuotaErrorDTO) response.getEntity();

            assertThat(errorDTO.limit()).isEqualTo(limit);
            assertThat(errorDTO.current()).isEqualTo(current);
            assertThat(errorDTO.requested()).isEqualTo(requested);
            assertThat(errorDTO.message()).isNotBlank();
        }
    }
}
