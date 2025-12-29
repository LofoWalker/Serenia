package com.lofo.serenia.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Stripe integration configuration.
 * Values are read from application.properties or environment variables.
 */
@ConfigMapping(prefix = "stripe")
public interface StripeConfig {

    /**
     * Stripe API secret key (sk_test_xxx or sk_live_xxx).
     */
    @WithName("api.key")
    String apiKey();

    /**
     * Secret for Stripe webhook signature validation.
     */
    @WithName("webhook.secret")
    @WithDefault("")
    String webhookSecret();

    /**
     * Redirect URL after successful payment.
     */
    @WithName("success.url")
    @WithDefault("http://localhost:4200/profile?payment=success")
    String successUrl();

    /**
     * Redirect URL after payment cancellation.
     */
    @WithName("cancel.url")
    @WithDefault("http://localhost:4200/profile?payment=cancel")
    String cancelUrl();
}

