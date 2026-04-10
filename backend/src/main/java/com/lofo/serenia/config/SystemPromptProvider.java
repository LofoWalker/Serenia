package com.lofo.serenia.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Loads the system prompt from prompt.md on the classpath at application startup.
 * Fails fast if the file is missing or unreadable.
 */
@Slf4j
@ApplicationScoped
public class SystemPromptProvider {

    private static final String PROMPT_RESOURCE = "prompt.md";

    @Getter
    private String systemPrompt;

    void onStart(@Observes StartupEvent ev) {
        this.systemPrompt = loadFromClasspath();
        log.info("System prompt loaded successfully ({} characters)", systemPrompt.length());
    }

    private String loadFromClasspath() {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(PROMPT_RESOURCE)) {
            if (is == null) {
                throw new IllegalStateException(
                    "Required file '%s' not found in classpath. Application cannot start without a system prompt."
                        .formatted(PROMPT_RESOURCE)
                );
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to read '%s' from classpath: %s".formatted(PROMPT_RESOURCE, e.getMessage()), e
            );
        }
    }
}

