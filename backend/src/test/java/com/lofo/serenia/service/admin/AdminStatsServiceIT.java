package com.lofo.serenia.service.admin;

import com.lofo.serenia.TestResourceProfile;
import com.lofo.serenia.persistence.entity.conversation.Message;
import com.lofo.serenia.persistence.entity.conversation.MessageRole;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.user.Role;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.MessageRepository;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.rest.dto.out.admin.*;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@TestProfile(TestResourceProfile.class)
@DisplayName("AdminStatsService Integration Tests")
class AdminStatsServiceIT {

    @Inject
    AdminStatsService adminStatsService;

    @Inject
    UserRepository userRepository;

    @Inject
    MessageRepository messageRepository;

    @Inject
    SubscriptionRepository subscriptionRepository;

    @Inject
    PlanRepository planRepository;

    @BeforeEach
    @Transactional
    void setup() {
        messageRepository.deleteAll();
        subscriptionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("should return empty stats when no data")
    void should_return_empty_stats_when_no_data() {
        DashboardDTO dashboard = adminStatsService.getDashboard();

        assertThat(dashboard.users().totalUsers()).isZero();
        assertThat(dashboard.messages().totalUserMessages()).isZero();
        assertThat(dashboard.engagement().activeUsers()).isZero();
    }

    @Test
    @DisplayName("should count users correctly")
    @Transactional
    void should_count_users_correctly() {
        createUser("user1@test.com", true);
        createUser("user2@test.com", true);
        createUser("inactive@test.com", false);

        UserStatsDTO stats = adminStatsService.getUserStats();

        assertThat(stats.totalUsers()).isEqualTo(3);
        assertThat(stats.activatedUsers()).isEqualTo(2);
    }

    @Test
    @DisplayName("should count only USER role messages")
    @Transactional
    void should_count_only_user_role_messages() {
        User user = createUser("user@test.com", true);
        UUID conversationId = UUID.randomUUID();

        createMessage(user.getId(), conversationId, MessageRole.USER);
        createMessage(user.getId(), conversationId, MessageRole.USER);
        createMessage(user.getId(), conversationId, MessageRole.ASSISTANT);

        MessageStatsDTO stats = adminStatsService.getMessageStats();

        assertThat(stats.totalUserMessages()).isEqualTo(2);
    }

    @Test
    @DisplayName("should calculate engagement stats")
    @Transactional
    void should_calculate_engagement_stats() {
        User activeUser = createUser("active@test.com", true);
        createUser("inactive@test.com", true);
        UUID conversationId = UUID.randomUUID();

        createMessage(activeUser.getId(), conversationId, MessageRole.USER);
        createMessage(activeUser.getId(), conversationId, MessageRole.USER);

        EngagementStatsDTO stats = adminStatsService.getEngagementStats();

        assertThat(stats.activeUsers()).isEqualTo(1);
        assertThat(stats.activationRate()).isEqualTo(50.0);
        assertThat(stats.avgMessagesPerUser()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("should count users by plan")
    @Transactional
    void should_count_users_by_plan() {
        User user1 = createUser("free@test.com", true);
        User user2 = createUser("plus@test.com", true);

        Plan freePlan = planRepository.findByName(PlanType.FREE).orElseThrow();
        Plan plusPlan = planRepository.findByName(PlanType.PLUS).orElseThrow();

        createSubscription(user1, freePlan);
        createSubscription(user2, plusPlan);

        UserStatsDTO stats = adminStatsService.getUserStats();

        assertThat(stats.freeUsers()).isEqualTo(1);
        assertThat(stats.plusUsers()).isEqualTo(1);
        assertThat(stats.maxUsers()).isZero();
    }

    @Test
    @DisplayName("should return timeline data")
    @Transactional
    void should_return_timeline_data() {
        User user = createUser("user@test.com", true);
        UUID conversationId = UUID.randomUUID();
        createMessage(user.getId(), conversationId, MessageRole.USER);

        TimelineDTO timeline = adminStatsService.getTimeline("messages", 7);

        assertThat(timeline.metric()).isEqualTo("messages");
        assertThat(timeline.data()).hasSize(7);
    }

    private User createUser(String email, boolean activated) {
        User user = User.builder()
                .email(email)
                .password("hashedpassword")
                .firstName("Test")
                .lastName("User")
                .accountActivated(activated)
                .role(Role.USER)
                .build();
        userRepository.persist(user);
        return user;
    }

    private void createMessage(UUID userId, UUID conversationId, MessageRole role) {
        Message message = new Message();
        message.setUserId(userId);
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setEncryptedContent(new byte[]{1, 2, 3});
        message.setTimestamp(Instant.now());
        messageRepository.persist(message);
    }

    private void createSubscription(User user, Plan plan) {
        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .tokensUsedThisMonth(0)
                .messagesSentToday(0)
                .monthlyPeriodStart(LocalDateTime.now())
                .dailyPeriodStart(LocalDateTime.now())
                .build();
        subscriptionRepository.persist(subscription);
    }
}

