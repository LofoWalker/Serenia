package com.lofo.serenia.service.subscription;

import com.lofo.serenia.exception.exceptions.QuotaExceededException;
import com.lofo.serenia.exception.exceptions.QuotaType;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuotaService Tests")
class QuotaServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private TokenCountingService tokenCountingService;

    private QuotaService quotaService;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        quotaService = new QuotaService(subscriptionRepository, tokenCountingService);
        Plan freePlan = Plan.builder()
                .id(UUID.randomUUID())
                .name(PlanType.FREE)
                .perMessageTokenLimit(1000)
                .monthlyTokenLimit(10000)
                .dailyMessageLimit(10)
                .build();
        User user = User.builder()
                .id(USER_ID)
                .email("test@example.com")
                .build();
        subscription = Subscription.builder()
                .id(SUBSCRIPTION_ID)
                .user(user)
                .plan(freePlan)
                .tokensUsedThisMonth(0)
                .messagesSentToday(0)
                .monthlyPeriodStart(LocalDateTime.now())
                .dailyPeriodStart(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("checkQuotaBeforeCall")
    class CheckQuotaBeforeCall {

        @Test
        @DisplayName("should allow when quotas are OK")
        void should_allow_when_quotas_ok() {
            when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                    .thenReturn(Optional.of(subscription));
            assertDoesNotThrow(() -> quotaService.checkQuotaBeforeCall(USER_ID));
        }

        @Test
        @DisplayName("should reject when monthly tokens exhausted")
        void should_reject_when_monthly_tokens_exhausted() {
            subscription.setTokensUsedThisMonth(10000);
            when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                    .thenReturn(Optional.of(subscription));
            QuotaExceededException exception = assertThrows(
                    QuotaExceededException.class,
                    () -> quotaService.checkQuotaBeforeCall(USER_ID)
            );
            assertEquals(QuotaType.MONTHLY_TOKEN_LIMIT, exception.getQuotaType());
        }

        @Test
        @DisplayName("should reject when daily message limit reached")
        void should_reject_when_daily_message_limit_reached() {
            subscription.setMessagesSentToday(10);
            when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                    .thenReturn(Optional.of(subscription));
            QuotaExceededException exception = assertThrows(
                    QuotaExceededException.class,
                    () -> quotaService.checkQuotaBeforeCall(USER_ID)
            );
            assertEquals(QuotaType.DAILY_MESSAGE_LIMIT, exception.getQuotaType());
        }

        @Test
        @DisplayName("should throw exception when subscription not found")
        void should_throw_exception_when_subscription_not_found() {
            when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                    .thenReturn(Optional.empty());
            assertThrows(IllegalStateException.class,
                    () -> quotaService.checkQuotaBeforeCall(USER_ID));
        }

        @Test
        @DisplayName("should reset daily counter when period expired")
        void should_reset_daily_counter_when_period_expired() {
            subscription.setMessagesSentToday(10);
            subscription.setDailyPeriodStart(LocalDateTime.now().minusDays(2));
            when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                    .thenReturn(Optional.of(subscription));
            assertDoesNotThrow(() -> quotaService.checkQuotaBeforeCall(USER_ID));
            assertEquals(0, subscription.getMessagesSentToday());
        }

        @Test
        @DisplayName("should reset monthly counter when period expired")
        void should_reset_monthly_counter_when_period_expired() {
            subscription.setTokensUsedThisMonth(10000);
            subscription.setMonthlyPeriodStart(LocalDateTime.now().minusMonths(2));
            when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                    .thenReturn(Optional.of(subscription));
            assertDoesNotThrow(() -> quotaService.checkQuotaBeforeCall(USER_ID));
            assertEquals(0, subscription.getTokensUsedThisMonth());
        }
    }

    @Nested
    @DisplayName("recordUsage")
    class RecordUsage {

        @Test
        @DisplayName("should increment counters with strlen")
        void should_increment_counters_with_strlen() {
            String userMessage = "Hello";
            String assistantResponse = "Hi there!";
            when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                    .thenReturn(Optional.of(subscription));
            when(tokenCountingService.countExchangeTokens(userMessage, assistantResponse))
                    .thenReturn(14);
            quotaService.recordUsage(USER_ID, userMessage, assistantResponse);
            assertEquals(14, subscription.getTokensUsedThisMonth());
            assertEquals(1, subscription.getMessagesSentToday());
            verify(subscriptionRepository).persist(subscription);
        }

        @Test
        @DisplayName("should throw when subscription not found")
        void should_throw_when_subscription_not_found() {
            when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                    .thenReturn(Optional.empty());
            assertThrows(IllegalStateException.class,
                    () -> quotaService.recordUsage(USER_ID, "Hello", "Hi"));
        }
    }

    @Nested
    @DisplayName("canSendMessage")
    class CanSendMessage {

        @Test
        @DisplayName("should return true when quotas OK")
        void should_return_true_when_quotas_ok() {
            when(subscriptionRepository.findByUserId(USER_ID))
                    .thenReturn(Optional.of(subscription));
            assertTrue(quotaService.canSendMessage(USER_ID));
        }

        @Test
        @DisplayName("should return false when monthly tokens exhausted")
        void should_return_false_when_monthly_tokens_exhausted() {
            subscription.setTokensUsedThisMonth(10000);
            when(subscriptionRepository.findByUserId(USER_ID))
                    .thenReturn(Optional.of(subscription));
            assertFalse(quotaService.canSendMessage(USER_ID));
        }

        @Test
        @DisplayName("should return true when no subscription exists")
        void should_return_true_when_no_subscription() {
            when(subscriptionRepository.findByUserId(USER_ID))
                    .thenReturn(Optional.empty());
            assertTrue(quotaService.canSendMessage(USER_ID));
        }
    }
}
