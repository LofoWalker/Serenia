package com.lofo.serenia.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SystemPromptProvider tests")
class SystemPromptProviderTest {

    @Test
    @DisplayName("Should load system prompt from classpath resource")
    void should_load_system_prompt_from_classpath() throws Exception {
        SystemPromptProvider provider = new SystemPromptProvider();
        SereniaConfig config = mock(SereniaConfig.class);
        when(config.systemPromptPath()).thenReturn("classpath:prompt.md");
        setSereniaConfig(provider, config);

        provider.onStart(null);

        assertEquals("Test system prompt content", provider.getSystemPrompt().trim());
    }

    @Test
    @DisplayName("Should load system prompt from filesystem path")
    void should_load_system_prompt_from_filesystem(@TempDir Path tempDir) throws Exception {
        Path promptFile = tempDir.resolve("prompt.md");
        Files.writeString(promptFile, "Filesystem prompt content");

        SystemPromptProvider provider = new SystemPromptProvider();
        SereniaConfig config = mock(SereniaConfig.class);
        when(config.systemPromptPath()).thenReturn(promptFile.toString());
        setSereniaConfig(provider, config);

        provider.onStart(null);

        assertEquals("Filesystem prompt content", provider.getSystemPrompt());
    }

    @Test
    @DisplayName("Should fail when classpath resource is missing")
    void should_fail_when_classpath_resource_missing() throws Exception {
        SystemPromptProvider provider = new SystemPromptProvider();
        SereniaConfig config = mock(SereniaConfig.class);
        when(config.systemPromptPath()).thenReturn("classpath:nonexistent.md");
        setSereniaConfig(provider, config);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> provider.onStart(null));
        assertTrue(ex.getMessage().contains("not found in classpath"));
    }

    @Test
    @DisplayName("Should fail when filesystem file is missing")
    void should_fail_when_filesystem_file_missing() throws Exception {
        SystemPromptProvider provider = new SystemPromptProvider();
        SereniaConfig config = mock(SereniaConfig.class);
        when(config.systemPromptPath()).thenReturn("/nonexistent/path/prompt.md");
        setSereniaConfig(provider, config);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> provider.onStart(null));
        assertTrue(ex.getMessage().contains("not found on filesystem"));
    }

    private void setSereniaConfig(SystemPromptProvider provider, SereniaConfig config) throws Exception {
        Field field = SystemPromptProvider.class.getDeclaredField("sereniaConfig");
        field.setAccessible(true);
        field.set(provider, config);
    }
}
