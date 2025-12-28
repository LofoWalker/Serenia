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
 * Service de gestion des quotas d'utilisation.
 * Gère la vérification et l'enregistrement de la consommation des utilisateurs.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class QuotaService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final TokenCountingService tokenCountingService;

    /**
     * Vérifie que l'utilisateur dispose encore de quota avant un appel.
     * Réinitialise les périodes expirées si nécessaire.
     *
     * @param userId l'identifiant de l'utilisateur
     * @throws QuotaExceededException si une limite est atteinte
     */
    @Transactional
    public void checkQuotaBeforeCall(UUID userId) {
        Subscription subscription = getOrCreateSubscriptionForUpdate(userId);
        resetExpiredPeriods(subscription);

        validateMonthlyTokenLimit(userId, subscription);
        validateDailyMessageLimit(userId, subscription);

        logQuotaStatus(userId, subscription);
    }

    /**
     * Enregistre l'utilisation d'un échange (message utilisateur + réponse).
     *
     * @param userId l'identifiant de l'utilisateur
     * @param userMessage le message de l'utilisateur
     * @param assistantResponse la réponse de l'assistant
     */
    @Transactional
    public void recordUsage(UUID userId, String userMessage, String assistantResponse) {
        Subscription subscription = getSubscriptionForUpdateOrThrow(userId);

        int tokensUsed = tokenCountingService.countExchangeTokens(userMessage, assistantResponse);
        updateUsageCounters(subscription, tokensUsed);

        subscriptionRepository.persist(subscription);

        log.debug("Recorded usage for user {}: {} tokens (total: {}), {} messages today",
                userId, tokensUsed, subscription.getTokensUsedThisMonth(),
                subscription.getMessagesSentToday());
    }

    /**
     * Vérifie si l'utilisateur peut envoyer un message.
     * Méthode non-bloquante qui ne lève pas d'exception.
     *
     * @param userId l'identifiant de l'utilisateur
     * @return true si l'utilisateur peut envoyer un message
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

    // ========== Méthodes privées ==========

    private Subscription getOrCreateSubscriptionForUpdate(UUID userId) {
        return subscriptionRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> subscriptionService.createDefaultSubscription(userId));
    }

    private Subscription getSubscriptionForUpdateOrThrow(UUID userId) {
        return subscriptionRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("Subscription not found for user: " + userId));
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
