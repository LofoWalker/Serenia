package com.lofo.serenia.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads the system prompt from a configurable location at application startup.
 * Supports both classpath (classpath:prompt.md) and filesystem (/opt/serenia/prompt.md) paths.
 * Fails fast if the file is missing or unreadable.
 */
@Slf4j
@ApplicationScoped
public class SystemPromptProvider {

    private static final String CLASSPATH_PREFIX = "classpath:";

    private final SereniaConfig sereniaConfig;

    @Getter
    private String systemPrompt;

    @Inject
    public SystemPromptProvider(SereniaConfig sereniaConfig) {
        this.sereniaConfig = sereniaConfig;
    }

    void onStart(@Observes StartupEvent ev) {
        String promptPath = sereniaConfig.systemPromptPath();
        this.systemPrompt = loadPrompt(promptPath);
        log.info("System prompt loaded successfully from '{}' ({} characters)", promptPath, systemPrompt.length());
    }

    private String loadPrompt(String promptPath) {
        if (promptPath.startsWith(CLASSPATH_PREFIX)) {
            return loadFromClasspath(promptPath.substring(CLASSPATH_PREFIX.length()));
        }
        return loadFromFilesystem(promptPath);
    }

    private String loadFromClasspath(String resourcePath) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException(
                    "Required file '%s' not found in classpath. Application cannot start without a system prompt."
                        .formatted(resourcePath)
                );
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to read '%s' from classpath: %s".formatted(resourcePath, e.getMessage()), e
            );
        }
    }

    private String loadFromFilesystem(String filePath) {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            throw new IllegalStateException(
                "Required file '%s' not found on filesystem. Application cannot start without a system prompt."
                    .formatted(filePath)
            );
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to read '%s' from filesystem: %s".formatted(filePath, e.getMessage()), e
            );
        }
    }
}

