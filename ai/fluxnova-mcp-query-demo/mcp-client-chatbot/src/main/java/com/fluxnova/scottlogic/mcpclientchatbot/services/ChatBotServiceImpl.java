package com.fluxnova.scottlogic.mcpclientchatbot.services;

import com.fluxnova.scottlogic.mcpclientchatbot.models.ChatRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ChatBotServiceImpl implements ChatBotService {

    private final ChatClient chatClient;
    private final BasePromptProvider basePromptProvider;

    public ChatBotServiceImpl(ChatClient chatClient, BasePromptProvider basePromptProvider) {
        this.chatClient = chatClient;
        this.basePromptProvider = basePromptProvider;
    }

    public String chat(ChatRequest chatRequest) {
        UUID chatId = Optional
                .ofNullable(chatRequest.chatId())
                .orElse(UUID.randomUUID());
        String basePrompt = basePromptProvider.getBasePrompt();
        return chatClient
            .prompt()
            .system(basePrompt)
            .user(chatRequest.question())
            .advisors(advisorSpec -> advisorSpec.param("chat_memory_conversation_id", chatId))
            .call()
            .content();
    }
}
