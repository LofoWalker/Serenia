package com.lofo.serenia.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Strongly typed mapping for the "serenia" configuration namespace.
 */
@ConfigMapping(prefix = "serenia")
public interface SereniaConfig {

    /**
     * Secret used to sign or encrypt sensitive data flowing through the platform.
     */
    @WithName("security.key")
    String securityKey();

    /**
     * Default maximum number of input tokens granted to each user.
     */
    @WithName("tokens.input-limit-default")
    @WithDefault("100000")
    Long defaultInputTokensLimit();

    /**
     * Default maximum number of output tokens granted to each user.
     */
    @WithName("tokens.output-limit-default")
    @WithDefault("100000")
    Long defaultOutputTokensLimit();

    /**
     * Default aggregated token quota for a user (input + output).
     */
    @WithName("tokens.total-limit-default")
    @WithDefault("200000")
    Long defaultTotalTokensLimit();

    /**
     * Maximum number of user accounts the platform is allowed to host.
     */
    @WithName("auth.max-users")
    @WithDefault("200")
    Long maxUsers();

    /**
     * Expiration window (in minutes) for email verification tokens.
     */
    @WithName("email-verification.token-expiration-minutes")
    @WithDefault("1440")
    long emailVerificationTokenExpirationMinutes();

    /**
     * System prompt injected into the LLM interactions.
     */
    @WithName("system-prompt")
    String systemPrompt();

    /**
     * URL of the server
     */
    @WithName("url")
    String url();

    /**
     * URL of the frontend application
     */
    @WithName("front-url")
    String frontUrl();

    /**
     * JWT issuer for token generation and validation.
     */
    @WithName("auth.jwt-issuer")
    String jwtIssuer();

    /**
     * Token expiration time in seconds.
     */
    @WithName("auth.expiration-time")
    @WithDefault("3600")
    Long tokenExpirationTime();
}
