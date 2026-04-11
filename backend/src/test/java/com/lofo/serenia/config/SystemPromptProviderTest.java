package com.lofo.serenia.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SystemPromptProvider tests")
class SystemPromptProviderTest {

    @Test
    @DisplayName("Should load system prompt from classpath resource")
    void should_load_system_prompt_from_classpath() {
        var provider = new SystemPromptProvider();

        provider.onStart(null);

        assertNotNull(provider.getSystemPrompt());
        assertFalse(provider.getSystemPrompt().isBlank());
    }
}
