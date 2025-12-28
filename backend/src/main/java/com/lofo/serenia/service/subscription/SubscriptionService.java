package com.lofo.serenia.service.subscription;

import com.lofo.serenia.exception.exceptions.SereniaException;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.rest.dto.out.PlanDTO;
import com.lofo.serenia.rest.dto.out.SubscriptionStatusDTO;
import com.lofo.serenia.service.user.shared.UserFinder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User subscription management service.
 * Handles subscription creation, retrieval, and modification.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final UserFinder userFinder;

    /**
     * Creates a subscription with the default FREE plan.
     *
     * @param userId the user identifier
     * @return the created subscription
     */
    @Transactional
    public Subscription createDefaultSubscription(UUID userId) {
        return createSubscription(userId, PlanType.FREE);
    }

    /**
     * Creates a subscription for a user with the specified plan.
     *
     * @param userId   the user identifier
     * @param planType the plan type to associate
     * @return the created subscription
     * @throws SereniaException if the user already has a subscription or doesn't exist
     */
    @Transactional
    public Subscription createSubscription(UUID userId, PlanType planType) {
        validateNoExistingSubscription(userId);

        User user = userFinder.findByIdOrThrow(userId);
        Plan plan = findPlanOrThrow(planType);

        Subscription subscription = buildSubscription(user, plan);
        subscriptionRepository.persist(subscription);

        log.info("Created subscription for user {} with plan {}", userId, planType);
        return subscription;
    }

    /**
     * Retrieves a user's subscription.
     * The subscription must exist (created at registration).
     *
     * @param userId the user identifier
     * @return the user's subscription
     * @throws SereniaException if the subscription doesn't exist
     */
    @Transactional
    public Subscription getSubscription(UUID userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("Subscription not found for user {} - this should never happen", userId);
                    return SereniaException.notFound("Subscription not found for user: " + userId);
                });
    }

    /**
     * Changes a user's plan.
     *
     * @param userId      the user identifier
     * @param newPlanType the new plan type
     * @return the updated subscription
     * @throws SereniaException if the subscription or plan doesn't exist
     */
    @Transactional
    public Subscription changePlan(UUID userId, PlanType newPlanType) {
        Subscription subscription = getSubscription(userId);
        Plan newPlan = findPlanOrThrow(newPlanType);

        PlanType oldPlanType = subscription.getPlan().getName();
        subscription.setPlan(newPlan);
        subscriptionRepository.persist(subscription);

        log.info("Changed plan for user {} from {} to {}", userId, oldPlanType, newPlanType);
        return subscription;
    }

    /**
     * Retrieves the complete subscription status of a user.
     *
     * @param userId the user identifier
     * @return the DTO containing the subscription status
     */
    @Transactional
    public SubscriptionStatusDTO getStatus(UUID userId) {
        Subscription subscription = getSubscription(userId);
        return buildStatusDTO(subscription);
    }

    /**
     * Retrieves the list of all available plans.
     *
     * @return the list of plans as DTOs
     */
    public List<PlanDTO> getAllPlans() {
        return planRepository.listAll().stream()
                .map(PlanDTO::from)
                .toList();
    }

    private void validateNoExistingSubscription(UUID userId) {
        if (subscriptionRepository.existsByUserId(userId)) {
            throw SereniaException.conflict("User already has a subscription");
        }
    }

    private Plan findPlanOrThrow(PlanType planType) {
        return planRepository.findByName(planType)
                .orElseThrow(() -> SereniaException.notFound("Plan not found: " + planType));
    }

    private Subscription buildSubscription(User user, Plan plan) {
        LocalDateTime now = LocalDateTime.now();
        return Subscription.builder()
                .user(user)
                .plan(plan)
                .tokensUsedThisMonth(0)
                .messagesSentToday(0)
                .monthlyPeriodStart(now)
                .dailyPeriodStart(now)
                .build();
    }

    private SubscriptionStatusDTO buildStatusDTO(Subscription subscription) {
        Plan plan = subscription.getPlan();

        int tokensRemaining = Math.max(0,
                plan.getMonthlyTokenLimit() - subscription.getTokensUsedThisMonth());
        int messagesRemaining = Math.max(0,
                plan.getDailyMessageLimit() - subscription.getMessagesSentToday());

        LocalDateTime monthlyResetDate = subscription.getMonthlyPeriodStart().plusMonths(1);
        LocalDateTime dailyResetDate = subscription.getDailyPeriodStart().plusDays(1);

        boolean hasStripeSubscription = subscription.getStripeSubscriptionId() != null
                && !subscription.getStripeSubscriptionId().isEmpty();

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
                dailyResetDate,
                subscription.getStatus().name(),
                subscription.getCurrentPeriodEnd(),
                subscription.getCancelAtPeriodEnd() != null && subscription.getCancelAtPeriodEnd(),
                plan.getPriceCents(),
                plan.getCurrency(),
                hasStripeSubscription
        );
    }
}
