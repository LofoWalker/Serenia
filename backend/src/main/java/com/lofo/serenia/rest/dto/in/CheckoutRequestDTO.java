package com.lofo.serenia.rest.dto.in;

import com.lofo.serenia.persistence.entity.subscription.PlanType;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for creating a Stripe Checkout session.
 */
public record CheckoutRequestDTO(
        @NotNull(message = "Plan type is required")
        PlanType planType
) {
}

