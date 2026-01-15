package com.lofo.serenia.service.admin;

import com.lofo.serenia.persistence.entity.conversation.MessageRole;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.repository.MessageRepository;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.persistence.repository.UserRepository;
import com.lofo.serenia.rest.dto.out.admin.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
@RequiredArgsConstructor
public class AdminStatsService {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final EntityManager entityManager;

    public DashboardDTO getDashboard() {
        return new DashboardDTO(
                getUserStats(),
                getMessageStats(),
                getEngagementStats(),
                getSubscriptionStats()
        );
    }

    public UserStatsDTO getUserStats() {
        long totalUsers = userRepository.count();
        long activatedUsers = userRepository.count("accountActivated", true);

        Instant now = Instant.now();
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        long newUsersLast7Days = userRepository.count("createdAt >= ?1", sevenDaysAgo);
        long newUsersLast30Days = userRepository.count("createdAt >= ?1", thirtyDaysAgo);

        long freeUsers = countUsersByPlan(PlanType.FREE);
        long plusUsers = countUsersByPlan(PlanType.PLUS);
        long maxUsers = countUsersByPlan(PlanType.MAX);

        return new UserStatsDTO(
                totalUsers,
                activatedUsers,
                freeUsers,
                plusUsers,
                maxUsers,
                newUsersLast7Days,
                newUsersLast30Days
        );
    }

    private static final String USER_MESSAGE_COUNT_QUERY = "role = ?1 AND timestamp >= ?2";

    public MessageStatsDTO getMessageStats() {
        long totalUserMessages = messageRepository.count("role", MessageRole.USER);

        Instant now = Instant.now();
        Instant startOfToday = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);

        long messagesToday = messageRepository.count(USER_MESSAGE_COUNT_QUERY, MessageRole.USER, startOfToday);
        long messagesLast7Days = messageRepository.count(USER_MESSAGE_COUNT_QUERY, MessageRole.USER, sevenDaysAgo);
        long messagesLast30Days = messageRepository.count(USER_MESSAGE_COUNT_QUERY, MessageRole.USER, thirtyDaysAgo);

        return new MessageStatsDTO(
                totalUserMessages,
                messagesToday,
                messagesLast7Days,
                messagesLast30Days
        );
    }

    public EngagementStatsDTO getEngagementStats() {
        long activatedUsers = userRepository.count("accountActivated", true);
        long activeUsers = countActiveUsers();
        double activationRate = activatedUsers > 0 ? (double) activeUsers / activatedUsers * 100 : 0;

        long totalUserMessages = messageRepository.count("role", MessageRole.USER);
        double avgMessagesPerUser = activeUsers > 0 ? (double) totalUserMessages / activeUsers : 0;

        return new EngagementStatsDTO(
                activeUsers,
                Math.round(activationRate * 100.0) / 100.0,
                Math.round(avgMessagesPerUser * 100.0) / 100.0
        );
    }

    public SubscriptionStatsDTO getSubscriptionStats() {
        Long totalTokens = entityManager
                .createQuery("SELECT COALESCE(SUM(s.tokensUsedThisMonth), 0) FROM Subscription s", Long.class)
                .getSingleResult();

        var plusPlan = planRepository.findByName(PlanType.PLUS);
        var maxPlan = planRepository.findByName(PlanType.MAX);

        long plusUsers = countUsersByPlan(PlanType.PLUS);
        long maxUsers = countUsersByPlan(PlanType.MAX);

        int plusRevenue = plusPlan.map(p -> (int) (plusUsers * p.getPriceCents())).orElse(0);
        int maxRevenue = maxPlan.map(p -> (int) (maxUsers * p.getPriceCents())).orElse(0);

        return new SubscriptionStatsDTO(
                totalTokens,
                plusRevenue + maxRevenue,
                "EUR"
        );
    }

    public TimelineDTO getTimeline(String metric, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays((long) days - 1);

        List<TimelineDataPointDTO> data = switch (metric) {
            case "users" -> getUserTimeline(startDate, endDate);
            case "messages" -> getMessageTimeline(startDate, endDate);
            default -> List.of();
        };

        return new TimelineDTO(metric, data);
    }

    @SuppressWarnings("unchecked")
    private List<TimelineDataPointDTO> getUserTimeline(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = entityManager.createNativeQuery(
                        "SELECT CAST(created_at AS DATE) as date, COUNT(*) as cnt " +
                                "FROM users WHERE created_at >= ?1 AND created_at < ?2 " +
                                "GROUP BY CAST(created_at AS DATE) ORDER BY date")
                .setParameter(1, startDate.atStartOfDay(ZoneOffset.UTC).toInstant())
                .setParameter(2, endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant())
                .getResultList();

        return fillMissingDatesNative(results, startDate, endDate);
    }

    @SuppressWarnings("unchecked")
    private List<TimelineDataPointDTO> getMessageTimeline(LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = entityManager.createNativeQuery(
                        "SELECT CAST(timestamp AS DATE) as date, COUNT(*) as cnt " +
                                "FROM messages WHERE role = ?1 AND timestamp >= ?2 AND timestamp < ?3 " +
                                "GROUP BY CAST(timestamp AS DATE) ORDER BY date")
                .setParameter(1, MessageRole.USER.name())
                .setParameter(2, startDate.atStartOfDay(ZoneOffset.UTC).toInstant())
                .setParameter(3, endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant())
                .getResultList();

        return fillMissingDatesNative(results, startDate, endDate);
    }

    private List<TimelineDataPointDTO> fillMissingDatesNative(List<Object[]> results, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Long> dataMap = results.stream()
                .collect(Collectors.toMap(
                        row -> toLocalDate(row[0]),
                        row -> ((Number) row[1]).longValue()
                ));

        List<TimelineDataPointDTO> filled = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            filled.add(new TimelineDataPointDTO(date, dataMap.getOrDefault(date, 0L)));
        }
        return filled;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate ld) {
            return ld;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.util.Date utilDate) {
            return utilDate.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        }
        throw new IllegalArgumentException("Cannot convert to LocalDate: " + value.getClass());
    }

    private long countUsersByPlan(PlanType planType) {
        return subscriptionRepository.count("plan.name", planType);
    }

    private long countActiveUsers() {
        return entityManager
                .createQuery("SELECT COUNT(DISTINCT m.userId) FROM Message m WHERE m.role = :role", Long.class)
                .setParameter("role", MessageRole.USER)
                .getSingleResult();
    }

    public UserListDTO getUserList(int page, int size) {
        long totalCount = userRepository.count();

        var users = userRepository.findAll()
                .page(page, size)
                .list()
                .stream()
                .map(this::toUserDetail)
                .toList();

        return new UserListDTO(users, totalCount, page, size);
    }

    public UserDetailDTO getUserByEmail(String email) {
        return userRepository.find("email", email)
                .firstResultOptional()
                .map(this::toUserDetail)
                .orElse(null);
    }

    private UserDetailDTO toUserDetail(com.lofo.serenia.persistence.entity.user.User user) {
        long messageCount = messageRepository.count("userId = ?1 AND role = ?2", user.getId(), MessageRole.USER);

        var subscription = subscriptionRepository.findByUserId(user.getId()).orElse(null);
        String planType = subscription != null ? subscription.getPlan().getName().name() : "NONE";
        int tokensUsed = subscription != null ? subscription.getTokensUsedThisMonth() : 0;
        int messagesSentToday = subscription != null ? subscription.getMessagesSentToday() : 0;

        return new UserDetailDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                planType,
                user.isAccountActivated(),
                user.getCreatedAt(),
                messageCount,
                tokensUsed,
                messagesSentToday
        );
    }
}

