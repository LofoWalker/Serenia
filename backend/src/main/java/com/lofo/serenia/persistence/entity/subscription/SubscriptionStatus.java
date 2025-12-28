package com.lofo.serenia.persistence.entity.subscription;

/**
 * Possible subscription statuses.
 * These statuses correspond to Stripe subscription states.
 */
public enum SubscriptionStatus {
    /**
     * Active subscription with payment up to date.
     */
    ACTIVE,

    /**
     * Canceled subscription, but still active until the end of the period.
     * User retains access until current_period_end.
     */
    CANCELED,

    /**
     * Payment failed, subscription is past due.
     * Stripe will automatically retry.
     */
    PAST_DUE,

    /**
     * Initial payment incomplete (awaiting 3D Secure, etc.).
     */
    INCOMPLETE,

    /**
     * Subscription expired after repeated payment failures.
     */
    UNPAID
}

