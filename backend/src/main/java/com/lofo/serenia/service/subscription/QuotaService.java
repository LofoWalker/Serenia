package com.lofo.serenia.service.subscription;

import com.lofo.serenia.exception.exceptions.QuotaExceededException;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * Service for managing usage quotas.
 * Handles verification and recording of user consumption.
 * Tokens are now recorded directly from the OpenAI API, without approximation.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class QuotaService {

    private final SubscriptionRepository subscriptionRepository;

    /**
     * Checks that the user still has available quota before a call.
     * Resets expired periods if necessary.
     *
     * @param userId the user identifier
     * @throws QuotaExceededException if a limit is reached
     */
    @Transactional
    public void checkQuotaBeforeCall(UUID userId) {
        Subscription subscription = getSubscriptionForUpdate(userId);
        resetExpiredPeriods(subscription);

        validateMonthlyTokenLimit(userId, subscription);
        validateDailyMessageLimit(userId, subscription);

        logQuotaStatus(userId, subscription);
    }

    /**
     * Records the actual token usage returned by the OpenAI API.
     *
     * @param userId the user identifier
     * @param actualTokensUsed the actual tokens consumed (from ChatCompletion.usage().totalTokens())
     */
    @Transactional
    public void recordUsage(UUID userId, int actualTokensUsed) {
        Subscription subscription = getSubscriptionForUpdate(userId);

        updateUsageCounters(subscription, actualTokensUsed);

        subscriptionRepository.persist(subscription);

        log.debug("Recorded usage for user {}: {} tokens (total: {}), {} messages today",
                userId, actualTokensUsed, subscription.getTokensUsedThisMonth(),
                subscription.getMessagesSentToday());
    }

    /**
     * Checks if the user can send a message.
     * Non-blocking method that does not throw exceptions.
     *
     * @param userId the user identifier
     * @return true if the user can send a message
     */
    public boolean canSendMessage(UUID userId) {
        try {
            return subscriptionRepository.findByUserId(userId)
                    .map(this::hasAvailableQuota)
                    .orElse(true);
        } catch (Exception e) {
            log.error("Error checking quota for user {}", userId, e);
            return false;
        }
    }

    // ========== Private methods ==========

    private Subscription getSubscriptionForUpdate(UUID userId) {
        return subscriptionRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> {
                    log.error("Subscription not found for user {} - this should never happen", userId);
                    return new IllegalStateException("Subscription not found for user: " + userId);
                });
    }

    private void validateMonthlyTokenLimit(UUID userId, Subscription subscription) {
        Plan plan = subscription.getPlan();
        if (subscription.getTokensUsedThisMonth() >= plan.getMonthlyTokenLimit()) {
            log.warn("User {} has exhausted monthly token limit: {}/{}",
                    userId, subscription.getTokensUsedThisMonth(), plan.getMonthlyTokenLimit());
            throw QuotaExceededException.monthlyTokenLimit(
                    plan.getMonthlyTokenLimit(),
                    subscription.getTokensUsedThisMonth(),
                    0
            );
        }
    }

    private void validateDailyMessageLimit(UUID userId, Subscription subscription) {
        Plan plan = subscription.getPlan();
        if (subscription.getMessagesSentToday() >= plan.getDailyMessageLimit()) {
            log.warn("User {} reached daily message limit: {}/{}",
                    userId, subscription.getMessagesSentToday(), plan.getDailyMessageLimit());
            throw QuotaExceededException.dailyMessageLimit(
                    plan.getDailyMessageLimit(),
                    subscription.getMessagesSentToday()
            );
        }
    }

    private void logQuotaStatus(UUID userId, Subscription subscription) {
        Plan plan = subscription.getPlan();
        log.debug("Quota check passed for user {}: {}/{} tokens, {}/{} messages",
                userId,
                subscription.getTokensUsedThisMonth(), plan.getMonthlyTokenLimit(),
                subscription.getMessagesSentToday(), plan.getDailyMessageLimit());
    }

    private void updateUsageCounters(Subscription subscription, int tokensUsed) {
        subscription.setTokensUsedThisMonth(subscription.getTokensUsedThisMonth() + tokensUsed);
        subscription.setMessagesSentToday(subscription.getMessagesSentToday() + 1);
    }

    private boolean hasAvailableQuota(Subscription subscription) {
        Plan plan = subscription.getPlan();
        return subscription.getTokensUsedThisMonth() < plan.getMonthlyTokenLimit()
                && subscription.getMessagesSentToday() < plan.getDailyMessageLimit();
    }

    private void resetExpiredPeriods(Subscription subscription) {
        boolean updated = false;

        if (subscription.isMonthlyPeriodExpired()) {
            log.info("Resetting monthly period for subscription {}", subscription.getId());
            subscription.resetMonthlyPeriod();
            updated = true;
        }

        if (subscription.isDailyPeriodExpired()) {
            log.info("Resetting daily period for subscription {}", subscription.getId());
            subscription.resetDailyPeriod();
            updated = true;
        }

        if (updated) {
            subscriptionRepository.persist(subscription);
        }
    }
}
