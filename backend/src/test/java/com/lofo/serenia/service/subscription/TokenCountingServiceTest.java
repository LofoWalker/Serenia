package com.lofo.serenia.service.subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
@DisplayName("TokenCountingService Tests")
class TokenCountingServiceTest {
    private TokenCountingService tokenCountingService;
    @BeforeEach
    void setUp() {
        tokenCountingService = new TokenCountingService();
    }
    @Nested
    @DisplayName("countTokens")
    class CountTokens {
        @Test
        @DisplayName("should return 0 for null content")
        void should_return_zero_for_null_content() {
            int result = tokenCountingService.countTokens(null);
            assertEquals(0, result);
        }
        @Test
        @DisplayName("should return 0 for empty content")
        void should_return_zero_for_empty_content() {
            int result = tokenCountingService.countTokens("");
            assertEquals(0, result);
        }
        @Test
        @DisplayName("should return string length for simple text")
        void should_return_string_length_for_simple_text() {
            String content = "Hello world";
            int result = tokenCountingService.countTokens(content);
            assertEquals(11, result);
        }
        @Test
        @DisplayName("should return correct length for unicode")
        void should_return_correct_length_for_unicode() {
            String content = "Héllo 你好";
            int result = tokenCountingService.countTokens(content);
            assertEquals(content.length(), result);
        }
    }
    @Nested
    @DisplayName("countExchangeTokens")
    class CountExchangeTokens {
        @Test
        @DisplayName("should sum both message lengths")
        void should_sum_both_message_lengths() {
            String userMessage = "Hello";
            String assistantResponse = "Hi there!";
            int result = tokenCountingService.countExchangeTokens(userMessage, assistantResponse);
            assertEquals(14, result);
        }
        @Test
        @DisplayName("should handle null user message")
        void should_handle_null_user_message() {
            int result = tokenCountingService.countExchangeTokens(null, "Response");
            assertEquals(8, result);
        }
        @Test
        @DisplayName("should handle null assistant response")
        void should_handle_null_assistant_response() {
            int result = tokenCountingService.countExchangeTokens("Hello", null);
            assertEquals(5, result);
        }
        @Test
        @DisplayName("should return 0 for both null")
        void should_return_zero_for_both_null() {
            int result = tokenCountingService.countExchangeTokens(null, null);
            assertEquals(0, result);
        }
    }
}
