package com.lofo.serenia.service.subscription;

import com.lofo.serenia.exception.exceptions.SereniaException;
import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;
import com.lofo.serenia.persistence.entity.subscription.Subscription;
import com.lofo.serenia.persistence.entity.user.User;
import com.lofo.serenia.persistence.repository.PlanRepository;
import com.lofo.serenia.persistence.repository.SubscriptionRepository;
import com.lofo.serenia.rest.dto.out.SubscriptionStatusDTO;
import com.lofo.serenia.service.user.shared.UserFinder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service de gestion des subscriptions utilisateurs.
 * Gère la création, récupération et modification des abonnements.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final UserFinder userFinder;

    /**
     * Crée une subscription avec le plan FREE par défaut.
     *
     * @param userId l'identifiant de l'utilisateur
     * @return la subscription créée
     */
    @Transactional
    public Subscription createDefaultSubscription(UUID userId) {
        return createSubscription(userId, PlanType.FREE);
    }

    /**
     * Crée une subscription pour un utilisateur avec le plan spécifié.
     *
     * @param userId   l'identifiant de l'utilisateur
     * @param planType le type de plan à associer
     * @return la subscription créée
     * @throws SereniaException si l'utilisateur a déjà une subscription ou n'existe pas
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
     * Récupère la subscription d'un utilisateur.
     * La subscription doit exister (créée à l'inscription).
     *
     * @param userId l'identifiant de l'utilisateur
     * @return la subscription de l'utilisateur
     * @throws SereniaException si la subscription n'existe pas
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
     * Change le plan d'un utilisateur.
     *
     * @param userId      l'identifiant de l'utilisateur
     * @param newPlanType le nouveau type de plan
     * @return la subscription mise à jour
     * @throws SereniaException si la subscription ou le plan n'existe pas
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
     * Récupère le statut complet de la subscription d'un utilisateur.
     *
     * @param userId l'identifiant de l'utilisateur
     * @return le DTO contenant le statut de la subscription
     */
    @Transactional
    public SubscriptionStatusDTO getStatus(UUID userId) {
        Subscription subscription = getSubscription(userId);
        return buildStatusDTO(subscription);
    }

    // ========== Méthodes privées ==========

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
