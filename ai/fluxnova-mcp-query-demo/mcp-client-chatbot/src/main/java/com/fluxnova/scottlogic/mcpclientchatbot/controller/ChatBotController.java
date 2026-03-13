package com.fluxnova.scottlogic.mcpclientchatbot.controller;

import com.fluxnova.scottlogic.mcpclientchatbot.models.ChatRequest;
import com.fluxnova.scottlogic.mcpclientchatbot.services.ChatBotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
public class ChatBotController {

    private final ChatBotService chatBotService;

    public ChatBotController(ChatBotService chatBotService) {
        this.chatBotService = chatBotService;
    }

    @PostMapping("/chat")
    ResponseEntity<String> chat(@RequestBody ChatRequest chatRequest) {
        String chatResponse = chatBotService.chat(chatRequest);
        return ResponseEntity.ok(chatResponse);
    }
}
