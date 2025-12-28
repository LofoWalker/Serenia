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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service de gestion des subscriptions utilisateurs.
 */
@Slf4j
@ApplicationScoped
@AllArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final UserRepository userRepository;

    @Transactional
    public Subscription createDefaultSubscription(UUID userId) {
        return createSubscription(userId, PlanType.FREE);
    }

    @Transactional
    public Subscription createSubscription(UUID userId, PlanType planType) {

        if (subscriptionRepository.existsByUserId(userId)) {
            throw SereniaException.conflict("User already has a subscription");
        }

        User user = userRepository.find("id", userId).firstResult();

        if (user == null) {
            throw SereniaException.notFound("User not found: " + userId);
        }

        Plan plan = planRepository.findByName(planType).orElseThrow(() -> SereniaException.notFound("Plan not found: " + planType));
        LocalDateTime now = LocalDateTime.now();

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .tokensUsedThisMonth(0)
                .messagesSentToday(0)
                .monthlyPeriodStart(now)
                .dailyPeriodStart(now)
                .build();

        subscriptionRepository.persist(subscription);
        log.info("Created subscription for user {} with plan {}", userId, planType);
        return subscription;
    }

    @Transactional
    public Subscription getOrCreateSubscription(UUID userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSubscription(userId));
    }

    @Transactional
    public Subscription changePlan(UUID userId, PlanType newPlanType) {
        Subscription subscription = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> SereniaException.notFound("Subscription not found for user: " + userId));
        Plan newPlan = planRepository.findByName(newPlanType)
                .orElseThrow(() -> SereniaException.notFound("Plan not found: " + newPlanType));
        PlanType oldPlanType = subscription.getPlan().getName();
        subscription.setPlan(newPlan);
        subscriptionRepository.persist(subscription);
        log.info("Changed plan for user {} from {} to {}", userId, oldPlanType, newPlanType);
        return subscription;
    }

    public SubscriptionStatusDTO getStatus(UUID userId) {
        Subscription subscription = getOrCreateSubscription(userId);
        Plan plan = subscription.getPlan();
        int tokensRemaining = Math.max(0, 
                plan.getMonthlyTokenLimit() - subscription.getTokensUsedThisMonth());
        int messagesRemaining = Math.max(0, 
                plan.getDailyMessageLimit() - subscription.getMessagesSentToday());
        LocalDateTime monthlyResetDate = subscription.getMonthlyPeriodStart().plusMonths(1);
        LocalDateTime dailyResetDate = subscription.getDailyPeriodStart().plusDays(1);
        return new SubscriptionStatusDTO(
                plan.getName().name(),
                tokensRemaining,
                messagesRemaining,
                plan.getPerMessageTokenLimit(),
                plan.getMonthlyTokenLimit(),
                plan.getDailyMessageLimit(),
                subscription.getTokensUsedThisMonth(),
                subscription.getMessagesSentToday(),
                monthlyResetDate,
                dailyResetDate
        );
    }
}
