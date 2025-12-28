# Phase 6 : Tests Unitaires

**Durée estimée:** 3-4h

**Prérequis:** Phases 1 à 5 complétées

---

## 6.1 TokenCountingServiceTest

### Fichier à créer

`src/test/java/com/lofo/serenia/service/subscription/TokenCountingServiceTest.java`

### Implémentation

```java
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
            assertEquals(11, result); // strlen("Hello world") = 11
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
            String userMessage = "Hello";      // 5
            String assistantResponse = "Hi there!"; // 9
            
            int result = tokenCountingService.countExchangeTokens(userMessage, assistantResponse);
            
            assertEquals(14, result);
        }

        @Test
        @DisplayName("should handle null user message")
        void should_handle_null_user_message() {
            int result = tokenCountingService.countExchangeTokens(null, "Response");
            assertEquals(8, result); // strlen("Response")
        }

        @Test
        @DisplayName("should handle null assistant response")
        void should_handle_null_assistant_response() {
            int result = tokenCountingService.countExchangeTokens("Hello", null);
            assertEquals(5, result); // strlen("Hello")
        }

        @Test
        @DisplayName("should return 0 for both null")
        void should_return_zero_for_both_null() {
            int result = tokenCountingService.countExchangeTokens(null, null);
            assertEquals(0, result);
        }
    }
}

            assertEquals(expectedMin, result);
        }
    }

    @Nested
    @DisplayName("estimateRequestTokens")
    class EstimateRequestTokens {

        @Test
        @DisplayName("should estimate complete request tokens")
        void should_estimate_complete_request_tokens() {
---

## 6.2 QuotaServiceTest

### Fichier à créer

`src/test/java/com/lofo/serenia/service/subscription/QuotaServiceTest.java`

### Implémentation

```java
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuotaService Tests")
class QuotaServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private TokenCountingService tokenCountingService;

    private QuotaService quotaService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();

    private Plan freePlan;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        quotaService = new QuotaService(subscriptionRepository, subscriptionService, tokenCountingService);

        freePlan = Plan.builder()
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
            subscription.setTokensUsedThisMonth(10000); // At limit
            
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
            subscription.setMessagesSentToday(10); // At limit
            
            when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                    .thenReturn(Optional.of(subscription));

            QuotaExceededException exception = assertThrows(
                    QuotaExceededException.class,
                    () -> quotaService.checkQuotaBeforeCall(USER_ID)
            );

            assertEquals(QuotaType.DAILY_MESSAGE_LIMIT, exception.getQuotaType());
        }

        @Test
        @DisplayName("should create subscription if not exists")
        void should_create_subscription_if_not_exists() {
            when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                    .thenReturn(Optional.empty());
            when(subscriptionService.createDefaultSubscription(USER_ID))
                    .thenReturn(subscription);

            assertDoesNotThrow(() -> quotaService.checkQuotaBeforeCall(USER_ID));

            verify(subscriptionService).createDefaultSubscription(USER_ID);
        }

        @Test
        @DisplayName("should reset daily counter when period expired")
        void should_reset_daily_counter_when_period_expired() {
            subscription.setMessagesSentToday(10);
            subscription.setDailyPeriodStart(LocalDateTime.now().minusDays(2));
            
            when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                    .thenReturn(Optional.of(subscription));

            assertDoesNotThrow(() -> quotaService.checkQuotaBeforeCall(USER_ID));
            
            // Counter should be reset
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
            
            // Counter should be reset
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
            String assistantResponse = "Hi there, how can I help?";
            
            when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                    .thenReturn(Optional.of(subscription));
            when(tokenCountingService.countExchangeTokens(userMessage, assistantResponse))
                    .thenReturn(30); // 5 + 25

            quotaService.recordUsage(USER_ID, userMessage, assistantResponse);

            assertEquals(30, subscription.getTokensUsedThisMonth());
            assertEquals(1, subscription.getMessagesSentToday());
            verify(subscriptionRepository).persist(subscription);
        }

        @Test
        @DisplayName("should accumulate tokens over multiple calls")
        void should_accumulate_tokens_over_multiple_calls() {
            subscription.setTokensUsedThisMonth(100);
            subscription.setMessagesSentToday(2);
            
            when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                    .thenReturn(Optional.of(subscription));
            when(tokenCountingService.countExchangeTokens(any(), any()))
                    .thenReturn(50);

            quotaService.recordUsage(USER_ID, "msg", "response");

            assertEquals(150, subscription.getTokensUsedThisMonth());
            assertEquals(3, subscription.getMessagesSentToday());
        }

        @Test
        @DisplayName("should throw if subscription not found")
        void should_throw_if_subscription_not_found() {
            when(subscriptionRepository.findByUserIdForUpdate(USER_ID))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class,
                    () -> quotaService.recordUsage(USER_ID, "msg", "response"));
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
        @DisplayName("should return false when monthly limit exceeded")
        void should_return_false_when_monthly_limit_exceeded() {
            subscription.setTokensUsedThisMonth(10000);
            
            when(subscriptionRepository.findByUserId(USER_ID))
                    .thenReturn(Optional.of(subscription));

            assertFalse(quotaService.canSendMessage(USER_ID));
        }

        @Test
        @DisplayName("should return false when daily limit reached")
        void should_return_false_when_daily_limit_reached() {
            subscription.setMessagesSentToday(10);
            
            when(subscriptionRepository.findByUserId(USER_ID))
                    .thenReturn(Optional.of(subscription));

            assertFalse(quotaService.canSendMessage(USER_ID));
        }

        @Test
        @DisplayName("should return true if no subscription exists")
        void should_return_true_if_no_subscription_exists() {
            when(subscriptionRepository.findByUserId(USER_ID))
                    .thenReturn(Optional.empty());

            assertTrue(quotaService.canSendMessage(USER_ID));
        }
    }
}
```

---

## 6.3 SubscriptionServiceTest

### Fichier à créer

`src/test/java/com/lofo/serenia/service/subscription/SubscriptionServiceTest.java`

### Implémentation

```java
package com.lofo.serenia.service.subscription;

import com.lofo.serenia.exception.exceptions.SereniaException;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.rest.dto.out.SubscriptionStatusDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService Tests")
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private static final UUID USER_ID = UUID.randomUUID();
    
    private User user;
    private Plan freePlan;
    private Plan plusPlan;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        freePlan = Plan.builder()
                .id(UUID.randomUUID())
                .name(PlanType.FREE)
                .perMessageTokenLimit(1000)
                .monthlyTokenLimit(10000)
                .dailyMessageLimit(10)
                .build();

        plusPlan = Plan.builder()
                .id(UUID.randomUUID())
                .name(PlanType.PLUS)
                .perMessageTokenLimit(4000)
                .monthlyTokenLimit(100000)
                .dailyMessageLimit(50)
                .build();
    }

    @Nested
    @DisplayName("createDefaultSubscription")
    class CreateDefaultSubscription {

        @Test
        @DisplayName("should create subscription with FREE plan")
        void should_create_subscription_with_free_plan() {
            when(subscriptionRepository.existsByUserId(USER_ID)).thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(user);
            when(planRepository.findByName(PlanType.FREE)).thenReturn(Optional.of(freePlan));

            Subscription result = subscriptionService.createDefaultSubscription(USER_ID);

            assertNotNull(result);
            assertEquals(user, result.getUser());
            assertEquals(freePlan, result.getPlan());
            assertEquals(0, result.getTokensUsedThisMonth());
            assertEquals(0, result.getMessagesSentToday());
            assertNotNull(result.getMonthlyPeriodStart());
            assertNotNull(result.getDailyPeriodStart());

            verify(subscriptionRepository).persist(any(Subscription.class));
        }

        @Test
        @DisplayName("should throw exception if user already has subscription")
        void should_throw_exception_if_user_already_has_subscription() {
            when(subscriptionRepository.existsByUserId(USER_ID)).thenReturn(true);

            SereniaException exception = assertThrows(
                    SereniaException.class,
                    () -> subscriptionService.createDefaultSubscription(USER_ID)
            );

            assertEquals(409, exception.getHttpStatus());
            assertTrue(exception.getMessage().contains("already has a subscription"));
        }

        @Test
        @DisplayName("should throw exception if user not found")
        void should_throw_exception_if_user_not_found() {
            when(subscriptionRepository.existsByUserId(USER_ID)).thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(null);

            SereniaException exception = assertThrows(
                    SereniaException.class,
                    () -> subscriptionService.createDefaultSubscription(USER_ID)
            );

            assertEquals(404, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("getOrCreateSubscription")
    class GetOrCreateSubscription {

        @Test
        @DisplayName("should return existing subscription")
        void should_return_existing_subscription() {
            Subscription existing = Subscription.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .plan(freePlan)
                    .build();

            when(subscriptionRepository.findByUserId(USER_ID))
                    .thenReturn(Optional.of(existing));

            Subscription result = subscriptionService.getOrCreateSubscription(USER_ID);

            assertEquals(existing, result);
            verify(subscriptionRepository, never()).persist(any());
        }

        @Test
        @DisplayName("should create subscription if not exists")
        void should_create_subscription_if_not_exists() {
            when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(subscriptionRepository.existsByUserId(USER_ID)).thenReturn(false);
            when(userRepository.findById(USER_ID)).thenReturn(user);
            when(planRepository.findByName(PlanType.FREE)).thenReturn(Optional.of(freePlan));

            Subscription result = subscriptionService.getOrCreateSubscription(USER_ID);

            assertNotNull(result);
            verify(subscriptionRepository).persist(any(Subscription.class));
        }
    }

    @Nested
    @DisplayName("changePlan")
    class ChangePlan {

        @Test
        @DisplayName("should change plan successfully")
        void should_change_plan_successfully() {
            Subscription subscription = Subscription.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .plan(freePlan)
                    .tokensUsedThisMonth(500)
                    .messagesSentToday(3)
                    .monthlyPeriodStart(LocalDateTime.now())
                    .dailyPeriodStart(LocalDateTime.now())
                    .build();

            when(subscriptionRepository.findByUserId(USER_ID))
                    .thenReturn(Optional.of(subscription));
            when(planRepository.findByName(PlanType.PLUS))
                    .thenReturn(Optional.of(plusPlan));

            Subscription result = subscriptionService.changePlan(USER_ID, PlanType.PLUS);

            assertEquals(plusPlan, result.getPlan());
            // Les compteurs ne doivent pas être reset
            assertEquals(500, result.getTokensUsedThisMonth());
            assertEquals(3, result.getMessagesSentToday());

            verify(subscriptionRepository).persist(subscription);
        }

        @Test
        @DisplayName("should throw exception if subscription not found")
        void should_throw_exception_if_subscription_not_found() {
            when(subscriptionRepository.findByUserId(USER_ID))
                    .thenReturn(Optional.empty());

            SereniaException exception = assertThrows(
                    SereniaException.class,
                    () -> subscriptionService.changePlan(USER_ID, PlanType.PLUS)
            );

            assertEquals(404, exception.getHttpStatus());
        }

        @Test
        @DisplayName("should throw exception if plan not found")
        void should_throw_exception_if_plan_not_found() {
            Subscription subscription = Subscription.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .plan(freePlan)
                    .build();

            when(subscriptionRepository.findByUserId(USER_ID))
                    .thenReturn(Optional.of(subscription));
            when(planRepository.findByName(PlanType.PLUS))
                    .thenReturn(Optional.empty());

            SereniaException exception = assertThrows(
                    SereniaException.class,
                    () -> subscriptionService.changePlan(USER_ID, PlanType.PLUS)
            );

            assertEquals(404, exception.getHttpStatus());
        }
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatus {

        @Test
        @DisplayName("should return correct status DTO")
        void should_return_correct_status_dto() {
            LocalDateTime now = LocalDateTime.now();
            Subscription subscription = Subscription.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .plan(freePlan)
                    .tokensUsedThisMonth(3000)
                    .messagesSentToday(7)
                    .monthlyPeriodStart(now)
                    .dailyPeriodStart(now)
                    .build();

            when(subscriptionRepository.findByUserId(USER_ID))
                    .thenReturn(Optional.of(subscription));

            SubscriptionStatusDTO status = subscriptionService.getStatus(USER_ID);

            assertEquals("FREE", status.planName());
            assertEquals(7000, status.tokensRemainingThisMonth()); // 10000 - 3000
            assertEquals(3, status.messagesRemainingToday()); // 10 - 7
            assertEquals(1000, status.perMessageTokenLimit());
            assertEquals(10000, status.monthlyTokenLimit());
            assertEquals(10, status.dailyMessageLimit());
            assertEquals(3000, status.tokensUsedThisMonth());
            assertEquals(7, status.messagesSentToday());
            assertNotNull(status.monthlyResetDate());
            assertNotNull(status.dailyResetDate());
        }

        @Test
        @DisplayName("should not return negative remaining quotas")
        void should_not_return_negative_remaining_quotas() {
            Subscription subscription = Subscription.builder()
                    .id(UUID.randomUUID())
                    .user(user)
                    .plan(freePlan)
                    .tokensUsedThisMonth(15000) // Over limit
                    .messagesSentToday(15) // Over limit
                    .monthlyPeriodStart(LocalDateTime.now())
                    .dailyPeriodStart(LocalDateTime.now())
                    .build();

            when(subscriptionRepository.findByUserId(USER_ID))
                    .thenReturn(Optional.of(subscription));

            SubscriptionStatusDTO status = subscriptionService.getStatus(USER_ID);

            assertEquals(0, status.tokensRemainingThisMonth());
            assertEquals(0, status.messagesRemainingToday());
        }
    }
}
```

---

## 6.4 Mise à jour de ChatOrchestratorTest

### Fichier à modifier

`src/test/java/com/lofo/serenia/service/chat/ChatOrchestratorTest.java`

### Modifications

```java
package com.lofo.serenia.service.chat;

import com.lofo.serenia.config.SereniaConfig;
import com.lofo.serenia.exception.exceptions.QuotaExceededException;
import com.lofo.serenia.persistence.entity.conversation.Conversation;
import com.lofo.serenia.persistence.entity.conversation.Message;
import com.lofo.serenia.persistence.entity.conversation.MessageRole;
import com.lofo.serenia.service.subscription.QuotaService;
import com.lofo.serenia.service.subscription.TokenEstimationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ChatOrchestrator tests")
class ChatOrchestratorTest {

    private static final UUID FIXED_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID FIXED_CONV_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Mock
    private ConversationService conversationService;

    @Mock
    private MessageService messageService;

    @Mock
    private ChatCompletionService chatCompletionService;

    @Mock
    private SereniaConfig sereniaConfig;

    @Mock
    private QuotaService quotaService;

    private ChatOrchestrator chatOrchestrator;

    @BeforeEach
    void setup() {
        chatOrchestrator = new ChatOrchestrator(
                conversationService, 
                messageService, 
                chatCompletionService,
                sereniaConfig,
                quotaService
        );
    }

    @Test
    @DisplayName("Should process user message and return assistant reply")
    void should_process_user_message_and_return_assistant_reply() {
        // Setup
        Conversation conv = new Conversation();
        conv.setId(FIXED_CONV_ID);

        when(conversationService.getOrCreateActiveConversation(FIXED_USER_ID)).thenReturn(conv);
        when(sereniaConfig.systemPrompt()).thenReturn("System prompt");
        when(messageService.decryptConversationMessages(FIXED_USER_ID, FIXED_CONV_ID))
                .thenReturn(Collections.emptyList());
        when(messageService.persistAssistantMessage(eq(FIXED_USER_ID), eq(FIXED_CONV_ID), nullable(String.class)))
                .thenReturn(messageWithRole(MessageRole.ASSISTANT));
        when(chatCompletionService.generateReply(anyString(), anyList()))
                .thenReturn("Assistant reply");

        // Execute
        ProcessedMessageResult result = chatOrchestrator.processUserMessage(FIXED_USER_ID, "Hello world");

        // Verify
        assertNotNull(result);
        assertEquals(FIXED_CONV_ID, result.conversationId());
        assertEquals("Assistant reply", result.assistantMessage().content());

        // Verify flow: check quota BEFORE LLM, record usage AFTER
        verify(quotaService).checkQuotaBeforeCall(FIXED_USER_ID);
        verify(chatCompletionService).generateReply(eq("System prompt"), anyList());
        verify(quotaService).recordUsage(FIXED_USER_ID, "Hello world", "Assistant reply");
    }

    @Test
    @DisplayName("Should check quota before calling LLM")
    void should_check_quota_before_calling_llm() {
        // Setup
        Conversation conv = new Conversation();
        conv.setId(FIXED_CONV_ID);

        when(conversationService.getOrCreateActiveConversation(FIXED_USER_ID)).thenReturn(conv);
        when(sereniaConfig.systemPrompt()).thenReturn("System prompt");
        when(messageService.decryptConversationMessages(FIXED_USER_ID, FIXED_CONV_ID))
                .thenReturn(Collections.emptyList());
        when(messageService.persistAssistantMessage(any(), any(), any()))
                .thenReturn(messageWithRole(MessageRole.ASSISTANT));
        when(chatCompletionService.generateReply(anyString(), anyList()))
                .thenReturn("Reply");

        // Execute
        chatOrchestrator.processUserMessage(FIXED_USER_ID, "Test message");

        // Verify order: quota check BEFORE LLM call, record AFTER
        var inOrder = inOrder(quotaService, chatCompletionService);
        inOrder.verify(quotaService).checkQuotaBeforeCall(FIXED_USER_ID);
        inOrder.verify(chatCompletionService).generateReply(anyString(), anyList());
        inOrder.verify(quotaService).recordUsage(eq(FIXED_USER_ID), eq("Test message"), eq("Reply"));
    }

    @Test
    @DisplayName("Should throw QuotaExceededException when quota is exceeded")
    void should_throw_quota_exceeded_when_limit_reached() {
        // Setup
        Conversation conv = new Conversation();
        conv.setId(FIXED_CONV_ID);

        when(conversationService.getOrCreateActiveConversation(FIXED_USER_ID)).thenReturn(conv);
        doThrow(QuotaExceededException.dailyMessageLimit(10, 10))
                .when(quotaService).checkQuotaBeforeCall(FIXED_USER_ID);

        // Execute & Verify
        assertThrows(QuotaExceededException.class,
                () -> chatOrchestrator.processUserMessage(FIXED_USER_ID, "Test"));

        // LLM should NOT be called
        verify(chatCompletionService, never()).generateReply(anyString(), anyList());
        // Usage should NOT be recorded
        verify(quotaService, never()).recordUsage(any(), any(), any());
    }

    @Test
    @DisplayName("Should not record usage when LLM call fails")
    void should_not_record_usage_when_llm_call_fails() {
        // Setup
        Conversation conv = new Conversation();
        conv.setId(FIXED_CONV_ID);

        when(conversationService.getOrCreateActiveConversation(FIXED_USER_ID)).thenReturn(conv);
        when(sereniaConfig.systemPrompt()).thenReturn("System prompt");
        when(messageService.decryptConversationMessages(FIXED_USER_ID, FIXED_CONV_ID))
                .thenReturn(Collections.emptyList());
        when(chatCompletionService.generateReply(anyString(), anyList()))
                .thenThrow(new RuntimeException("LLM error"));

        // Execute
        assertThrows(RuntimeException.class,
                () -> chatOrchestrator.processUserMessage(FIXED_USER_ID, "Test"));

        // Verify usage was NOT recorded (since LLM failed)
        verify(quotaService, never()).recordUsage(any(), any(), any());
    }

    private Message messageWithRole(MessageRole role) {
        Message message = new Message();
        message.setRole(role);
        return message;
    }
}
```

---

## 6.5 Tâches

- [ ] Créer le dossier `src/test/java/com/lofo/serenia/service/subscription/`
- [ ] Créer `TokenCountingServiceTest.java`
- [ ] Créer `QuotaServiceTest.java`
- [ ] Créer `SubscriptionServiceTest.java`
- [ ] Modifier `ChatOrchestratorTest.java`
- [ ] Exécuter tous les tests et vérifier qu'ils passent

---

## 6.6 Exécution des tests

```bash
# Exécuter tous les tests unitaires
./mvnw test

# Exécuter uniquement les tests de subscription
./mvnw test -Dtest="**/subscription/*Test"

# Exécuter avec rapport de couverture
./mvnw test jacoco:report
```

---

[← Phase précédente : API REST](./05-api-rest.md) | [Retour au README](./README.md) | [Phase suivante : Tests d'intégration →](./07-tests-integration.md)

