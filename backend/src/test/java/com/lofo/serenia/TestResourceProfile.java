package com.lofo.serenia;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class TestResourceProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.ofEntries(
                // Database configuration
                Map.entry("quarkus.datasource.db-kind", "h2"),
                Map.entry("quarkus.datasource.jdbc.url", "jdbc:h2:mem:serenia;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"),
                Map.entry("quarkus.datasource.username", "sa"),
                Map.entry("quarkus.datasource.password", "sa"),
                Map.entry("quarkus.datasource.jdbc.driver", "org.h2.Driver"),
                Map.entry("quarkus.datasource.jdbc.max-size", "16"),
                // CORS configuration
                Map.entry("quarkus.http.cors.enabled", "true"),
                Map.entry("quarkus.http.cors.methods", "GET,POST,PUT,DELETE,OPTIONS"),
                Map.entry("quarkus.http.cors.origins", "http://localhost:4200"),
                Map.entry("quarkus.http.cors.access-control-allow-credentials", "true"),
                Map.entry("quarkus.http.cors.headers", "Content-Type,Authorization,X-Requested-With,Origin,Accept"),
                Map.entry("quarkus.http.cors.exposed-headers", "Authorization"),
                // Health check configuration
                Map.entry("quarkus.smallrye-health.root-path", "/q/health"),
                // Auth configuration
                Map.entry("serenia.auth.max-users", "2"),
                Map.entry("serenia.auth.jwt-issuer", "serenia"),
                Map.entry("serenia.auth.expiration-time", "3600"),
                // OpenAI configuration
                Map.entry("openai.api.key", "sk-testtesttesttesttest"),
                Map.entry("openai.model", "gpt-4o-mini"),
                // JWT configuration
                Map.entry("smallrye.jwt.sign.key.location", "classpath:keys/privateKey.pem"),
                Map.entry("mp.jwt.verify.publickey.location", "classpath:keys/publicKey.pem"),
                Map.entry("mp.jwt.verify.issuer", "serenia"),
                Map.entry("mp.jwt.token.header", "Authorization"),
                // Mailer configuration
                Map.entry("quarkus.mailer.mock", "true"),
                Map.entry("quarkus.mailer.host", "localhost"),
                Map.entry("quarkus.mailer.ssl", "false"),
                Map.entry("quarkus.mailer.start-tls", "DISABLED"),
                // Token quotas configuration
                Map.entry("serenia.tokens.input-limit-default", "1000"),
                Map.entry("serenia.tokens.output-limit-default", "1000"),
                Map.entry("serenia.tokens.total-limit-default", "5000"),
                // Security configuration
                Map.entry("serenia.security.key", "0x7956703D86068BA71F75486CFBEE87AC5978E60AE57501573B7C09A49F7F6745"),
                // Application configuration
                Map.entry("serenia.system-prompt", "You are a helpful assistant for mental health support."),
                Map.entry("serenia.url", "http://localhost:8080"),
                Map.entry("serenia.front-url", "http://localhost:4200"));
    }

    @Override
    public String getConfigProfile() {
        return "test";
    }
}
