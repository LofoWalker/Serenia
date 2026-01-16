package com.lofo.serenia.rest.dto.in;

import com.lofo.serenia.persistence.entity.subscription.PlanType;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for plan change request.
 */
public record ChangePlanRequestDTO(
        @NotNull(message = "Plan type is required")
        PlanType planType
) {
}

