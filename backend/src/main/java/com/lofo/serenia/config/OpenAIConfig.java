package com.lofo.serenia.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "openai")
public interface OpenAIConfig {

    /**
     * API key used to authenticate with OpenAI endpoints.
     */
    @WithName("api.key")
    String apiKey();

    /**
     * Default model identifier leveraged for chat completions.
     */
    @WithName("model")
    @WithDefault("gpt-5-nano")
    String model();
}