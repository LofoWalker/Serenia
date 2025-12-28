package com.lofo.serenia.rest.dto.out;

import com.lofo.serenia.persistence.entity.subscription.Plan;
import com.lofo.serenia.persistence.entity.subscription.PlanType;

/**
 * DTO representing an available subscription plan.
 */
public record PlanDTO(
        PlanType type,
        String name,
        Integer monthlyTokenLimit,
        Integer dailyMessageLimit,
        Integer perMessageTokenLimit,
        Integer priceCents,
        String currency
) {
    public static PlanDTO from(Plan plan) {
        return new PlanDTO(
                plan.getName(),
                getDisplayName(plan.getName()),
                plan.getMonthlyTokenLimit(),
                plan.getDailyMessageLimit(),
                plan.getPerMessageTokenLimit(),
                plan.getPriceCents(),
                plan.getCurrency()
        );
    }

    private static String getDisplayName(PlanType type) {
        return switch (type) {
            case FREE -> "Gratuit";
            case PLUS -> "Plus";
            case MAX -> "Max";
        };
    }
}

