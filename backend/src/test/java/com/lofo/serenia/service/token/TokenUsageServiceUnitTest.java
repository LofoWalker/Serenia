package com.lofo.serenia.service.token;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.domain.user.User;
import com.lofo.serenia.domain.user.UserTokenQuota;
import com.lofo.serenia.domain.user.UserTokenUsage;
import com.lofo.serenia.repository.UserTokenQuotaRepository;
import com.lofo.serenia.repository.UserTokenUsageRepository;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenUsageService unit tests")
@Tag("unit")
class TokenUsageServiceUnitTest {

    @Mock
    private UserTokenQuotaRepository tokenQuotaRepository;

    @Mock
    private UserTokenUsageRepository tokenUsageRepository;

    @Mock
    private SereniaConfig sereniaConfig;

    private TokenUsageService tokenUsageService;

    private User testUser;
    private UserTokenQuota testQuota;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        lenient().when(sereniaConfig.defaultInputTokensLimit()).thenReturn(100000L);
        lenient().when(sereniaConfig.defaultOutputTokensLimit()).thenReturn(100000L);
        lenient().when(sereniaConfig.defaultTotalTokensLimit()).thenReturn(200000L);

        tokenUsageService = new TokenUsageService(tokenQuotaRepository, tokenUsageRepository, sereniaConfig);

        testUser = new User();
        testUser.setId(testUserId);
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setLastName("Doe");
        testUser.setFirstName("John");

        testQuota = UserTokenQuota.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .inputTokensLimit(100000L)
                .outputTokensLimit(100000L)
                .totalTokensLimit(200000L)
                .inputTokensUsed(0L)
                .outputTokensUsed(0L)
                .totalTokensUsed(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Initialize user token quota")
    @Tag("unit")
    class InitializeUserTokenQuotaTests {

        @Test
        @DisplayName("should_create_quota_with_default_limits_when_user_valid")
        void should_create_quota_with_default_limits_when_user_valid() {
            tokenUsageService.initializeUserTokenQuota(testUser);

            ArgumentCaptor<UserTokenQuota> captor = ArgumentCaptor.forClass(UserTokenQuota.class);
            verify(tokenQuotaRepository, times(1)).persist(captor.capture());

            UserTokenQuota capturedQuota = captor.getValue();
            assertNotNull(capturedQuota);
            assertEquals(testUser.getId(), capturedQuota.getUser().getId());
            assertEquals(100000L, capturedQuota.getInputTokensLimit());
            assertEquals(100000L, capturedQuota.getOutputTokensLimit());
            assertEquals(200000L, capturedQuota.getTotalTokensLimit());
            assertEquals(0L, capturedQuota.getInputTokensUsed());
            assertEquals(0L, capturedQuota.getOutputTokensUsed());
            assertEquals(0L, capturedQuota.getTotalTokensUsed());
        }

        @Test
        @DisplayName("should_set_timestamps_when_creating_quota")
        void should_set_timestamps_when_creating_quota() {
            LocalDateTime before = LocalDateTime.now();
            tokenUsageService.initializeUserTokenQuota(testUser);
            LocalDateTime after = LocalDateTime.now();

            ArgumentCaptor<UserTokenQuota> captor = ArgumentCaptor.forClass(UserTokenQuota.class);
            verify(tokenQuotaRepository).persist(captor.capture());

            UserTokenQuota capturedQuota = captor.getValue();
            assertNotNull(capturedQuota.getCreatedAt());
            assertNotNull(capturedQuota.getUpdatedAt());
            assertTrue(capturedQuota.getCreatedAt().isAfter(before.minusSeconds(1)));
            assertTrue(capturedQuota.getCreatedAt().isBefore(after.plusSeconds(1)));
            assertTrue(capturedQuota.getUpdatedAt().isAfter(before.minusSeconds(1)));
            assertTrue(capturedQuota.getUpdatedAt().isBefore(after.plusSeconds(1)));
        }

        @Test
        @DisplayName("should_persist_quota_via_repository")
        void should_persist_quota_via_repository() {
            tokenUsageService.initializeUserTokenQuota(testUser);

            verify(tokenQuotaRepository, times(1)).persist(any(UserTokenQuota.class));
            verifyNoMoreInteractions(tokenQuotaRepository);
        }
    }

    @Nested
    @DisplayName("Consume tokens")
    @Tag("unit")
    class ConsumeTokensTests {

        @BeforeEach
        void setupMocks() {
            lenient().when(tokenQuotaRepository.findByUserId(testUserId)).thenReturn(Optional.of(testQuota));
        }

        @Test
        @DisplayName("should_increment_counters_when_within_limit")
        void should_increment_counters_when_within_limit() {
            tokenUsageService.consumeTokens(testUserId, 100, 200, "CONVERSATION");

            ArgumentCaptor<UserTokenQuota> captor = ArgumentCaptor.forClass(UserTokenQuota.class);
            verify(tokenQuotaRepository).persist(captor.capture());

            UserTokenQuota updatedQuota = captor.getValue();
            assertEquals(100L, updatedQuota.getInputTokensUsed());
            assertEquals(200L, updatedQuota.getOutputTokensUsed());
            assertEquals(300L, updatedQuota.getTotalTokensUsed());
        }

        @Test
        @DisplayName("should_record_usage_when_consuming_tokens")
        void should_record_usage_when_consuming_tokens() {
            tokenUsageService.consumeTokens(testUserId, 50, 150, "CONVERSATION");

            ArgumentCaptor<UserTokenUsage> captor = ArgumentCaptor.forClass(UserTokenUsage.class);
            verify(tokenUsageRepository).persist(captor.capture());

            UserTokenUsage recordedUsage = captor.getValue();
            assertEquals(50L, recordedUsage.getInputTokens());
            assertEquals(150L, recordedUsage.getOutputTokens());
            assertEquals(200L, recordedUsage.getTotalTokens());
            assertEquals("CONVERSATION", recordedUsage.getUsageType());
        }

        @Test
        @DisplayName("should_throw_exception_when_input_limit_exceeded")
        void should_throw_exception_when_input_limit_exceeded() {
            WebApplicationException exception = assertThrows(WebApplicationException.class,
                    () -> tokenUsageService.consumeTokens(testUserId, 100001, 0, "CONVERSATION"));

            assertEquals(Response.Status.PAYMENT_REQUIRED.getStatusCode(), exception.getResponse().getStatus());
            assertTrue(exception.getMessage().contains("Token quota exceeded"));
        }

        @Test
        @DisplayName("should_throw_exception_when_output_limit_exceeded")
        void should_throw_exception_when_output_limit_exceeded() {
            WebApplicationException exception = assertThrows(WebApplicationException.class,
                    () -> tokenUsageService.consumeTokens(testUserId, 0, 100001, "CONVERSATION"));

            assertEquals(Response.Status.PAYMENT_REQUIRED.getStatusCode(), exception.getResponse().getStatus());
        }

        @Test
        @DisplayName("should_throw_exception_when_total_limit_exceeded")
        void should_throw_exception_when_total_limit_exceeded() {
            testQuota.setInputTokensUsed(150000L);
            testQuota.setTotalTokensUsed(150000L);

            WebApplicationException exception = assertThrows(WebApplicationException.class,
                    () -> tokenUsageService.consumeTokens(testUserId, 1, 1, "CONVERSATION"));

            assertEquals(Response.Status.PAYMENT_REQUIRED.getStatusCode(), exception.getResponse().getStatus());
        }

        @Test
        @DisplayName("should_allow_consumption_exactly_at_limit")
        void should_allow_consumption_exactly_at_limit() {
            tokenUsageService.consumeTokens(testUserId, 100000, 100000, "CONVERSATION");

            ArgumentCaptor<UserTokenQuota> captor = ArgumentCaptor.forClass(UserTokenQuota.class);
            verify(tokenQuotaRepository).persist(captor.capture());

            UserTokenQuota updatedQuota = captor.getValue();
            assertEquals(100000L, updatedQuota.getInputTokensUsed());
            assertEquals(100000L, updatedQuota.getOutputTokensUsed());
            assertEquals(200000L, updatedQuota.getTotalTokensUsed());
        }

        @Test
        @DisplayName("should_throw_exception_when_exceeding_exact_limit")
        void should_throw_exception_when_exceeding_exact_limit() {
            testQuota.setInputTokensUsed(100000L);
            testQuota.setOutputTokensUsed(100000L);
            testQuota.setTotalTokensUsed(200000L);

            WebApplicationException exception = assertThrows(WebApplicationException.class,
                    () -> tokenUsageService.consumeTokens(testUserId, 1, 0, "CONVERSATION"));

            assertEquals(Response.Status.PAYMENT_REQUIRED.getStatusCode(), exception.getResponse().getStatus());
            verify(tokenQuotaRepository, never()).persist(any(UserTokenQuota.class));
        }

        @Test
        @DisplayName("should_throw_not_found_when_quota_missing")
        void should_throw_not_found_when_quota_missing() {
            UUID unknownUserId = UUID.randomUUID();
            when(tokenQuotaRepository.findByUserId(unknownUserId)).thenReturn(Optional.empty());

            WebApplicationException exception = assertThrows(WebApplicationException.class,
                    () -> tokenUsageService.consumeTokens(unknownUserId, 100, 100, "CONVERSATION"));

            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), exception.getResponse().getStatus());
        }

        @Test
        @DisplayName("should_support_multiple_usage_types")
        void should_support_multiple_usage_types() {
            tokenUsageService.consumeTokens(testUserId, 10, 20, "CONVERSATION");

            ArgumentCaptor<UserTokenUsage> captor = ArgumentCaptor.forClass(UserTokenUsage.class);
            verify(tokenUsageRepository).persist(captor.capture());

            UserTokenUsage usage = captor.getValue();
            assertEquals("CONVERSATION", usage.getUsageType());

            reset(tokenQuotaRepository, tokenUsageRepository);
            when(tokenQuotaRepository.findByUserId(testUserId)).thenReturn(Optional.of(testQuota));

            tokenUsageService.consumeTokens(testUserId, 5, 15, "API_CALL");

            captor = ArgumentCaptor.forClass(UserTokenUsage.class);
            verify(tokenUsageRepository).persist(captor.capture());

            usage = captor.getValue();
            assertEquals("API_CALL", usage.getUsageType());
        }
    }

    @Nested
    @DisplayName("Get user token quota")
    @Tag("unit")
    class GetUserTokenQuotaTests {

        @Test
        @DisplayName("should_return_quota_when_found")
        void should_return_quota_when_found() {
            when(tokenQuotaRepository.findByUserId(testUserId)).thenReturn(Optional.of(testQuota));

            UserTokenQuota result = tokenUsageService.getUserTokenQuota(testUserId);

            assertNotNull(result);
            assertEquals(testQuota.getId(), result.getId());
            assertEquals(testUserId, result.getUser().getId());
            verify(tokenQuotaRepository, times(1)).findByUserId(testUserId);
        }

        @Test
        @DisplayName("should_throw_not_found_when_quota_missing")
        void should_throw_not_found_when_quota_missing() {
            UUID unknownUserId = UUID.randomUUID();
            when(tokenQuotaRepository.findByUserId(unknownUserId)).thenReturn(Optional.empty());

            WebApplicationException exception = assertThrows(WebApplicationException.class,
                    () -> tokenUsageService.getUserTokenQuota(unknownUserId));

            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), exception.getResponse().getStatus());
        }

        @Test
        @DisplayName("should_return_quota_with_current_usage")
        void should_return_quota_with_current_usage() {
            testQuota.setInputTokensUsed(1000L);
            testQuota.setOutputTokensUsed(2000L);
            testQuota.setTotalTokensUsed(3000L);

            when(tokenQuotaRepository.findByUserId(testUserId)).thenReturn(Optional.of(testQuota));

            UserTokenQuota result = tokenUsageService.getUserTokenQuota(testUserId);

            assertEquals(1000L, result.getInputTokensUsed());
            assertEquals(2000L, result.getOutputTokensUsed());
            assertEquals(3000L, result.getTotalTokensUsed());
        }
    }

    @Nested
    @DisplayName("Get user token usage by date range")
    @Tag("unit")
    class GetUserTokenUsageByDateRangeTests {

        @Test
        @DisplayName("should_delegate_to_repository")
        void should_delegate_to_repository() {
            LocalDateTime startDate = LocalDateTime.now().minusHours(1);
            LocalDateTime endDate = LocalDateTime.now();
            List<UserTokenUsage> expectedUsages = List.of();

            when(tokenUsageRepository.findByUserIdAndDateRange(testUserId, startDate, endDate))
                    .thenReturn(expectedUsages);

            List<UserTokenUsage> result = tokenUsageService.getUserTokenUsageByDateRange(testUserId, startDate, endDate);

            assertEquals(expectedUsages, result);
            verify(tokenUsageRepository, times(1)).findByUserIdAndDateRange(testUserId, startDate, endDate);
        }

        @Test
        @DisplayName("should_return_usages_from_repository")
        void should_return_usages_from_repository() {
            LocalDateTime startDate = LocalDateTime.now().minusHours(1);
            LocalDateTime endDate = LocalDateTime.now();

            UserTokenUsage usage1 = UserTokenUsage.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .inputTokens(100L)
                    .outputTokens(200L)
                    .totalTokens(300L)
                    .usageType("CONVERSATION")
                    .createdAt(LocalDateTime.now())
                    .build();

            UserTokenUsage usage2 = UserTokenUsage.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .inputTokens(50L)
                    .outputTokens(150L)
                    .totalTokens(200L)
                    .usageType("API_CALL")
                    .createdAt(LocalDateTime.now())
                    .build();

            List<UserTokenUsage> expectedUsages = List.of(usage1, usage2);

            when(tokenUsageRepository.findByUserIdAndDateRange(testUserId, startDate, endDate))
                    .thenReturn(expectedUsages);

            List<UserTokenUsage> result = tokenUsageService.getUserTokenUsageByDateRange(testUserId, startDate, endDate);

            assertEquals(2, result.size());
            assertEquals(expectedUsages, result);
        }

        @Test
        @DisplayName("should_return_empty_list_when_no_usage_found")
        void should_return_empty_list_when_no_usage_found() {
            LocalDateTime startDate = LocalDateTime.now().minusHours(1);
            LocalDateTime endDate = LocalDateTime.now();

            when(tokenUsageRepository.findByUserIdAndDateRange(testUserId, startDate, endDate))
                    .thenReturn(List.of());

            List<UserTokenUsage> result = tokenUsageService.getUserTokenUsageByDateRange(testUserId, startDate, endDate);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Get total tokens used in period")
    @Tag("unit")
    class GetTotalTokensUsedInPeriodTests {

        @Test
        @DisplayName("should_sum_all_tokens_in_period")
        void should_sum_all_tokens_in_period() {
            LocalDateTime startDate = LocalDateTime.now().minusHours(1);
            LocalDateTime endDate = LocalDateTime.now();

            UserTokenUsage usage1 = UserTokenUsage.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .inputTokens(100L)
                    .outputTokens(200L)
                    .totalTokens(300L)
                    .usageType("CONVERSATION")
                    .createdAt(LocalDateTime.now())
                    .build();

            UserTokenUsage usage2 = UserTokenUsage.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .inputTokens(50L)
                    .outputTokens(150L)
                    .totalTokens(200L)
                    .usageType("CONVERSATION")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(tokenUsageRepository.findByUserIdAndDateRange(testUserId, startDate, endDate))
                    .thenReturn(List.of(usage1, usage2));

            long totalUsed = tokenUsageService.getTotalTokensUsedInPeriod(testUserId, startDate, endDate);

            assertEquals(500L, totalUsed);
        }

        @Test
        @DisplayName("should_return_zero_when_no_usage_in_period")
        void should_return_zero_when_no_usage_in_period() {
            LocalDateTime startDate = LocalDateTime.now().minusHours(2);
            LocalDateTime endDate = LocalDateTime.now().minusHours(1);

            when(tokenUsageRepository.findByUserIdAndDateRange(testUserId, startDate, endDate))
                    .thenReturn(List.of());

            long totalUsed = tokenUsageService.getTotalTokensUsedInPeriod(testUserId, startDate, endDate);

            assertEquals(0L, totalUsed);
        }

        @Test
        @DisplayName("should_sum_multiple_usage_types")
        void should_sum_multiple_usage_types() {
            LocalDateTime startDate = LocalDateTime.now().minusHours(1);
            LocalDateTime endDate = LocalDateTime.now();

            UserTokenUsage usage1 = UserTokenUsage.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .totalTokens(100L)
                    .usageType("CONVERSATION")
                    .createdAt(LocalDateTime.now())
                    .build();

            UserTokenUsage usage2 = UserTokenUsage.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .totalTokens(50L)
                    .usageType("API_CALL")
                    .createdAt(LocalDateTime.now())
                    .build();

            UserTokenUsage usage3 = UserTokenUsage.builder()
                    .id(UUID.randomUUID())
                    .user(testUser)
                    .totalTokens(25L)
                    .usageType("OTHER")
                    .createdAt(LocalDateTime.now())
                    .build();

            when(tokenUsageRepository.findByUserIdAndDateRange(testUserId, startDate, endDate))
                    .thenReturn(List.of(usage1, usage2, usage3));

            long totalUsed = tokenUsageService.getTotalTokensUsedInPeriod(testUserId, startDate, endDate);

            assertEquals(175L, totalUsed);
        }
    }

    @Nested
    @DisplayName("Record token usage")
    @Tag("unit")
    class RecordTokenUsageTests {

        @Test
        @DisplayName("should_persist_usage_record")
        void should_persist_usage_record() {
            tokenUsageService.recordTokenUsage(testUser, 100, 200, "CONVERSATION");

            ArgumentCaptor<UserTokenUsage> captor = ArgumentCaptor.forClass(UserTokenUsage.class);
            verify(tokenUsageRepository, times(1)).persist(captor.capture());

            UserTokenUsage recordedUsage = captor.getValue();
            assertNotNull(recordedUsage);
            assertEquals(testUser, recordedUsage.getUser());
        }

        @Test
        @DisplayName("should_set_correct_values_in_record")
        void should_set_correct_values_in_record() {
            tokenUsageService.recordTokenUsage(testUser, 75, 125, "TEST_TYPE");

            ArgumentCaptor<UserTokenUsage> captor = ArgumentCaptor.forClass(UserTokenUsage.class);
            verify(tokenUsageRepository).persist(captor.capture());

            UserTokenUsage recordedUsage = captor.getValue();
            assertEquals(testUser.getId(), recordedUsage.getUser().getId());
            assertEquals(75L, recordedUsage.getInputTokens());
            assertEquals(125L, recordedUsage.getOutputTokens());
            assertEquals(200L, recordedUsage.getTotalTokens());
            assertEquals("TEST_TYPE", recordedUsage.getUsageType());
        }

        @Test
        @DisplayName("should_set_created_at_timestamp")
        void should_set_created_at_timestamp() {
            LocalDateTime before = LocalDateTime.now();
            tokenUsageService.recordTokenUsage(testUser, 10, 20, "CONVERSATION");
            LocalDateTime after = LocalDateTime.now();

            ArgumentCaptor<UserTokenUsage> captor = ArgumentCaptor.forClass(UserTokenUsage.class);
            verify(tokenUsageRepository).persist(captor.capture());

            UserTokenUsage recordedUsage = captor.getValue();
            assertNotNull(recordedUsage.getCreatedAt());
            assertTrue(recordedUsage.getCreatedAt().isAfter(before.minusSeconds(1)));
            assertTrue(recordedUsage.getCreatedAt().isBefore(after.plusSeconds(1)));
        }
    }
}

