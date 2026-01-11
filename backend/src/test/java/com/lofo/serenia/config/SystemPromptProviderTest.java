package com.lofo.serenia.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SystemPromptProvider tests")
class SystemPromptProviderTest {

    @Test
    @DisplayName("Should load system prompt from classpath resource")
    void should_load_system_prompt_from_classpath() {
        SereniaConfig config = mock(SereniaConfig.class);
        when(config.systemPromptPath()).thenReturn("classpath:prompt.md");
        SystemPromptProvider provider = new SystemPromptProvider(config);

        provider.onStart(null);

        assertEquals("Test system prompt content", provider.getSystemPrompt().trim());
    }

    @Test
    @DisplayName("Should load system prompt from filesystem path")
    void should_load_system_prompt_from_filesystem(@TempDir Path tempDir) throws Exception {
        Path promptFile = tempDir.resolve("prompt.md");
        Files.writeString(promptFile, "Filesystem prompt content");

        SereniaConfig config = mock(SereniaConfig.class);
        when(config.systemPromptPath()).thenReturn(promptFile.toString());
        SystemPromptProvider provider = new SystemPromptProvider(config);

        provider.onStart(null);

        assertEquals("Filesystem prompt content", provider.getSystemPrompt());
    }

    @Test
    @DisplayName("Should fail when classpath resource is missing")
    void should_fail_when_classpath_resource_missing() {
        SereniaConfig config = mock(SereniaConfig.class);
        when(config.systemPromptPath()).thenReturn("classpath:nonexistent.md");
        SystemPromptProvider provider = new SystemPromptProvider(config);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> provider.onStart(null));
        assertTrue(ex.getMessage().contains("not found in classpath"));
    }

    @Test
    @DisplayName("Should fail when filesystem file is missing")
    void should_fail_when_filesystem_file_missing() {
        SereniaConfig config = mock(SereniaConfig.class);
        when(config.systemPromptPath()).thenReturn("/nonexistent/path/prompt.md");
        SystemPromptProvider provider = new SystemPromptProvider(config);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> provider.onStart(null));
        assertTrue(ex.getMessage().contains("not found on filesystem"));
    }
}
