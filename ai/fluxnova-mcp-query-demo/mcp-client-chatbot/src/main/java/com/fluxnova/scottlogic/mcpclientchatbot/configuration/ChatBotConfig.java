package com.fluxnova.scottlogic.mcpclientchatbot.configuration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
@EnableConfigurationProperties(McpToolAllowlistProperties.class)
public class ChatBotConfig {

    @Autowired
    ChatMemory chatMemory;

    @Bean
    public ChatClient chatClient(
            ChatModel chatModel,
            SyncMcpToolCallbackProvider toolCallbackProvider,
            ChatMemory chatMemory,
            McpToolAllowlistProperties toolAllowlistProperties) {

        ToolCallback[] filteredToolCallbacks = Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .toArray(ToolCallback[]::new);

        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultToolCallbacks(toolCallbackProvider.getToolCallbacks())
                .build();
    }
}
