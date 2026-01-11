package com.lofo.serenia;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class TestResourceProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.ofEntries(
                Map.entry("quarkus.datasource.jdbc.url",
                        "jdbc:h2:mem:serenia;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS PUBLIC\\;SET REFERENTIAL_INTEGRITY FALSE"),
                Map.entry("quarkus.datasource.username", "sa"),
                Map.entry("quarkus.datasource.password", "sa"),
                Map.entry("quarkus.datasource.jdbc.driver", "org.h2.Driver"),
                Map.entry("quarkus.datasource.jdbc.max-size", "16"),
                Map.entry("serenia.auth.max-users", "2"),
                Map.entry("serenia.auth.jwt-issuer", "serenia"),
                Map.entry("serenia.auth.expiration-time", "3600"),
                Map.entry("openai.api.key", "sk-testtesttesttesttest"),
                Map.entry("openai.model", "gpt-4o-mini"),
                Map.entry("smallrye.jwt.sign.key.location", "classpath:keys/privateKey.pem"),
                Map.entry("mp.jwt.verify.publickey.location", "classpath:keys/publicKey.pem"),
                Map.entry("mp.jwt.verify.issuer", "serenia"),
                Map.entry("mp.jwt.token.header", "Authorization"),
                Map.entry("quarkus.mailer.mock", "true"),
                Map.entry("stripe.api.key", "sk_test_mock_stripe_key"),
                Map.entry("stripe.webhook.secret", "whsec_test_mock_secret"),
                Map.entry("stripe.success.url", "http://localhost:4200/profile?payment=success"),
                Map.entry("stripe.cancel.url", "http://localhost:4200/profile?payment=cancel"),
                Map.entry("serenia.tokens.input-limit-default", "8000"),
                Map.entry("serenia.tokens.output-limit-default", "2000"),
                Map.entry("serenia.tokens.total-limit-default", "10000"),
                Map.entry("serenia.security.key", "test-security-key-for-testing"),
                Map.entry("serenia.system-prompt-path", "classpath:prompt.md"));
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
