package com.fluxnova.scottlogic.mcpclientchatbot.models;

import jakarta.annotation.Nullable;

import java.util.UUID;

public record ChatRequest(@Nullable UUID chatId, String question) {
}
