package com.fairshare.chat.dto;

import com.fairshare.chat.model.ChatMessage;
import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID senderId,
        UUID recipientId,
        String content,
        String messageType,
        String stickerCode,
        Integer voiceDurationMs,
        boolean hasVoice,
        Instant createdAt,
        Instant readAt
) {
    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(message.getId(), message.getSenderId(), message.getRecipientId(),
                message.getContent(), message.getMessageType(), message.getStickerCode(),
                message.getVoiceDurationMs(), message.getVoiceData() != null && message.getVoiceData().length > 0,
                message.getCreatedAt(), message.getReadAt());
    }
}
