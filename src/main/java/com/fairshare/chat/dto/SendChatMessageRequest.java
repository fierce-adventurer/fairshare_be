package com.fairshare.chat.dto;

public record SendChatMessageRequest(
        String content,
        String messageType,
        String stickerCode
) {}
