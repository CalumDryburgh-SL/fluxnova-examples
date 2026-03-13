package com.fluxnova.scottlogic.mcpclientchatbot.services;

import com.fluxnova.scottlogic.mcpclientchatbot.models.ChatRequest;

public interface ChatBotService {
    String chat(ChatRequest chatRequest);
}
