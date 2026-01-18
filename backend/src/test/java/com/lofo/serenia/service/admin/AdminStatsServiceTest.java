package com.lofo.serenia.service.admin;

import com.lofo.serenia.persistence.entity.conversation.MessageRole;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.repository.MessageRepository;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.rest.dto.out.admin.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStatsService Unit Tests")
class AdminStatsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<Long> longQuery;

    private AdminStatsService adminStatsService;

    @BeforeEach
    void setUp() {
        adminStatsService = new AdminStatsService(
                userRepository,
                messageRepository,
                subscriptionRepository,
                planRepository,
                entityManager
        );
    }

    @Nested
    @DisplayName("getDashboard")
    class GetDashboard {

        @Test
        @DisplayName("should return complete dashboard with all stats")
        void should_return_complete_dashboard_with_all_stats() {
            when(userRepository.count()).thenReturn(100L);
            when(userRepository.count(eq("accountActivated"), eq(true))).thenReturn(80L);
            when(userRepository.count(eq("createdAt >= ?1"), (Object) any())).thenReturn(10L);
            when(subscriptionRepository.count(eq("plan.name"), any(PlanType.class))).thenReturn(50L);
            when(messageRepository.count(eq("role"), eq(MessageRole.USER))).thenReturn(500L);
            when(messageRepository.count(anyString(), eq(MessageRole.USER), any())).thenReturn(50L);
            when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
            when(longQuery.getSingleResult()).thenReturn(10000L);
            when(longQuery.setParameter(anyString(), any())).thenReturn(longQuery);
            when(planRepository.findByName(PlanType.PLUS)).thenReturn(Optional.of(createPlan(PlanType.PLUS, 999)));
            when(planRepository.findByName(PlanType.MAX)).thenReturn(Optional.of(createPlan(PlanType.MAX, 1999)));

            DashboardDTO dashboard = adminStatsService.getDashboard();

            assertThat(dashboard).isNotNull();
            assertThat(dashboard.users()).isNotNull();
            assertThat(dashboard.messages()).isNotNull();
            assertThat(dashboard.engagement()).isNotNull();
            assertThat(dashboard.subscriptions()).isNotNull();
        }
    }

    @Nested
    @DisplayName("getUserStats")
    class GetUserStats {

        @Test
        @DisplayName("should return user stats with correct counts")
        void should_return_user_stats_with_correct_counts() {
            when(userRepository.count()).thenReturn(100L);
            when(userRepository.count(eq("accountActivated"), eq(true))).thenReturn(80L);
            when(userRepository.count(eq("createdAt >= ?1"), (Object) any())).thenReturn(10L);
            when(subscriptionRepository.count(eq("plan.name"), eq(PlanType.FREE))).thenReturn(50L);
            when(subscriptionRepository.count(eq("plan.name"), eq(PlanType.PLUS))).thenReturn(30L);
            when(subscriptionRepository.count(eq("plan.name"), eq(PlanType.MAX))).thenReturn(20L);

            UserStatsDTO stats = adminStatsService.getUserStats();

            assertThat(stats.totalUsers()).isEqualTo(100L);
            assertThat(stats.activatedUsers()).isEqualTo(80L);
        }

        @Test
        @DisplayName("should count users by plan type")
        void should_count_users_by_plan_type() {
            when(userRepository.count()).thenReturn(100L);
            when(userRepository.count(eq("accountActivated"), eq(true))).thenReturn(80L);
            when(userRepository.count(eq("createdAt >= ?1"), (Object) any())).thenReturn(10L);
            when(subscriptionRepository.count(eq("plan.name"), eq(PlanType.FREE))).thenReturn(50L);
            when(subscriptionRepository.count(eq("plan.name"), eq(PlanType.PLUS))).thenReturn(30L);
            when(subscriptionRepository.count(eq("plan.name"), eq(PlanType.MAX))).thenReturn(20L);

            UserStatsDTO stats = adminStatsService.getUserStats();

            assertThat(stats.freeUsers()).isEqualTo(50L);
            assertThat(stats.plusUsers()).isEqualTo(30L);
            assertThat(stats.maxUsers()).isEqualTo(20L);
        }
    }

    @Nested
    @DisplayName("getMessageStats")
    class GetMessageStats {

        @Test
        @DisplayName("should return message stats for different periods")
        void should_return_message_stats_for_different_periods() {
            when(messageRepository.count(eq("role"), eq(MessageRole.USER))).thenReturn(1000L);
            when(messageRepository.count(anyString(), eq(MessageRole.USER), any()))
                    .thenReturn(50L, 200L, 400L);

            MessageStatsDTO stats = adminStatsService.getMessageStats();

            assertThat(stats.totalUserMessages()).isEqualTo(1000L);
            assertThat(stats.messagesToday()).isEqualTo(50L);
            assertThat(stats.messagesLast7Days()).isEqualTo(200L);
            assertThat(stats.messagesLast30Days()).isEqualTo(400L);
        }
    }

    @Nested
    @DisplayName("getEngagementStats")
    class GetEngagementStats {

        @Test
        @DisplayName("should calculate activation rate correctly")
        void should_calculate_activation_rate_correctly() {
            when(userRepository.count(eq("accountActivated"), eq(true))).thenReturn(100L);
            when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
            when(longQuery.setParameter(anyString(), any())).thenReturn(longQuery);
            when(longQuery.getSingleResult()).thenReturn(80L);
            when(messageRepository.count(eq("role"), eq(MessageRole.USER))).thenReturn(500L);

            EngagementStatsDTO stats = adminStatsService.getEngagementStats();

            assertThat(stats.activeUsers()).isEqualTo(80L);
            assertThat(stats.activationRate()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("should return zero when no users")
        void should_return_zero_when_no_users() {
            when(userRepository.count(eq("accountActivated"), eq(true))).thenReturn(0L);
            when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
            when(longQuery.setParameter(anyString(), any())).thenReturn(longQuery);
            when(longQuery.getSingleResult()).thenReturn(0L);
            when(messageRepository.count(eq("role"), eq(MessageRole.USER))).thenReturn(0L);

            EngagementStatsDTO stats = adminStatsService.getEngagementStats();

            assertThat(stats.activationRate()).isEqualTo(0.0);
            assertThat(stats.avgMessagesPerUser()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("getSubscriptionStats")
    class GetSubscriptionStats {

        @Test
        @DisplayName("should calculate total revenue from plans")
        void should_calculate_total_revenue_from_plans() {
            when(entityManager.createQuery(anyString(), eq(Long.class))).thenReturn(longQuery);
            when(longQuery.getSingleResult()).thenReturn(50000L);
            when(planRepository.findByName(PlanType.PLUS)).thenReturn(Optional.of(createPlan(PlanType.PLUS, 999)));
            when(planRepository.findByName(PlanType.MAX)).thenReturn(Optional.of(createPlan(PlanType.MAX, 1999)));
            when(subscriptionRepository.count(eq("plan.name"), eq(PlanType.PLUS))).thenReturn(10L);
            when(subscriptionRepository.count(eq("plan.name"), eq(PlanType.MAX))).thenReturn(5L);

            SubscriptionStatsDTO stats = adminStatsService.getSubscriptionStats();

            assertThat(stats.totalTokensConsumed()).isEqualTo(50000L);
            assertThat(stats.estimatedRevenueCents()).isEqualTo(10 * 999 + 5 * 1999);
            assertThat(stats.currency()).isEqualTo("EUR");
        }
    }

    @Nested
    @DisplayName("getTimeline")
    class GetTimeline {

        @Test
        @DisplayName("should return empty for unknown metric")
        void should_return_empty_for_unknown_metric() {
            TimelineDTO timeline = adminStatsService.getTimeline("unknown", 7);

            assertThat(timeline.metric()).isEqualTo("unknown");
            assertThat(timeline.data()).isEmpty();
        }
    }

    private Plan createPlan(PlanType type, int priceCents) {
        Plan plan = new Plan();
        plan.setName(type);
        plan.setPriceCents(priceCents);
        plan.setMonthlyTokenLimit(10000);
        plan.setDailyMessageLimit(10);
        plan.setCurrency("EUR");
        return plan;
    }
}
