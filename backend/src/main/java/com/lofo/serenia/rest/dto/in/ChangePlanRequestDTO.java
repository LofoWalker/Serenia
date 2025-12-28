package com.lofo.serenia.rest.dto.in;

import com.lofo.serenia.persistence.entity.subscription.PlanType;
import jakarta.validation.constraints.NotNull;

/**
 * DTO pour la demande de changement de plan.
 */
public record ChangePlanRequestDTO(
        @NotNull(message = "Plan type is required")
        PlanType planType
) {
}

