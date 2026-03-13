package com.fluxnova.scottlogic.mcpclientchatbot.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class BasePromptProvider {

    private final Resource basePromptResource;
    private String basePrompt;

    public BasePromptProvider(@Value("classpath:prompts/base-prompt.md") Resource basePromptResource) {
        this.basePromptResource = basePromptResource;
    }

    @PostConstruct
    void loadBasePrompt() {
        this.basePrompt = readResource(basePromptResource).trim();
    }

    public String getBasePrompt() {
        return basePrompt;
    }

    private static String readResource(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load base prompt from " + resource.getDescription(), ex);
        }
    }
}
