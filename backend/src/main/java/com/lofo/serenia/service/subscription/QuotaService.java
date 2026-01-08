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
     * Records the token usage with cost normalization.
     * Raw tokens are logged for monitoring, normalized tokens are stored for billing.
     *
     * @param userId the user identifier
     * @param promptTokens total input tokens (from usage.promptTokens())
     * @param cachedTokens cached input tokens (from usage.promptTokensDetails().cachedTokens())
     * @param completionTokens output tokens (from usage.completionTokens())
     */
    @Transactional
    public void recordUsage(UUID userId, int promptTokens, int cachedTokens, int completionTokens) {
        Subscription subscription = getSubscriptionForUpdate(userId);

        int normalizedTokens = normalizeTokens(promptTokens, cachedTokens, completionTokens);

        updateUsageCounters(subscription, normalizedTokens);

        subscriptionRepository.persist(subscription);

        log.debug("Token usage for user {} - Raw [prompt: {}, cached: {}, completion: {}] | Normalized: {} | Monthly total: {}",
                userId, promptTokens, cachedTokens, completionTokens,
                normalizedTokens, subscription.getTokensUsedThisMonth());
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

    /**
     * Normalizes consumed tokens to "input tokens" equivalent for uniform cost calculation.
     * Based on GPT-4o-mini pricing:
     * - Input: $0.15 / 1M → factor 1
     * - Cached: $0.075 / 1M → factor 0.5 (2 cached = 1 input)
     * - Output: $0.60 / 1M → factor 4 (1 output = 4 input)
     *
     * <p>Note: Integer division may cause minor truncation (e.g., 501/2 = 250 instead of 250.5).
     * This is negligible at the per-million-token pricing scale and does not impact billing accuracy.</p>
     */
    private int normalizeTokens(int promptTokens, int cachedTokens, int completionTokens) {
        validateTokenParameters(promptTokens, cachedTokens, completionTokens);

        int nonCachedInput = promptTokens - cachedTokens;
        int cachedNormalized = cachedTokens / 2;
        int outputNormalized = completionTokens * 4;
        return nonCachedInput + cachedNormalized + outputNormalized;
    }

    private void validateTokenParameters(int promptTokens, int cachedTokens, int completionTokens) {
        if (promptTokens < 0) {
            log.warn("Invalid promptTokens value: {}. Treating as 0.", promptTokens);
            throw new IllegalArgumentException("promptTokens cannot be negative: " + promptTokens);
        }
        if (cachedTokens < 0) {
            log.warn("Invalid cachedTokens value: {}. Treating as 0.", cachedTokens);
            throw new IllegalArgumentException("cachedTokens cannot be negative: " + cachedTokens);
        }
        if (completionTokens < 0) {
            log.warn("Invalid completionTokens value: {}. Treating as 0.", completionTokens);
            throw new IllegalArgumentException("completionTokens cannot be negative: " + completionTokens);
        }
        if (cachedTokens > promptTokens) {
            log.warn("cachedTokens ({}) exceeds promptTokens ({}). This indicates an API anomaly.", cachedTokens, promptTokens);
            throw new IllegalArgumentException("cachedTokens (" + cachedTokens + ") cannot exceed promptTokens (" + promptTokens + ")");
        }
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
