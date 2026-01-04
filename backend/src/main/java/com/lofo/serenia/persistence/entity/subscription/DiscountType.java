package com.lofo.serenia.persistence.entity.subscription;

/**
 * Represents the type of discount applied via Stripe promotion code.
 * Values directly correspond to Stripe coupon discount types.
 */
public enum DiscountType {
    /**
     * Percentage-based discount (e.g., 10%).
     */
    PERCENTAGE,

    /**
     * Fixed amount discount (e.g., €5).
     */
    AMOUNT
}

