package com.lofo.serenia.rest.dto.out;

import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;

/**
 * DTO représentant un plan d'abonnement disponible.
 */
public record PlanDTO(
        PlanType type,
        String name,
        Integer monthlyTokenLimit,
        Integer dailyMessageLimit,
        Integer perMessageTokenLimit
) {
    /**
     * Crée un PlanDTO à partir d'une entité Plan.
     */
    public static PlanDTO from(Plan plan) {
        return new PlanDTO(
                plan.getName(),
                getDisplayName(plan.getName()),
                plan.getMonthlyTokenLimit(),
                plan.getDailyMessageLimit(),
                plan.getPerMessageTokenLimit()
        );
    }

    /**
     * Retourne le nom d'affichage du plan.
     */
    private static String getDisplayName(PlanType type) {
        return switch (type) {
            case FREE -> "Gratuit";
            case PLUS -> "Plus";
            case MAX -> "Max";
        };
    }
}

