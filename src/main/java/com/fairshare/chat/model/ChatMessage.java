package com.fairshare.chat.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id private UUID id;
    @Column(name = "sender_id", nullable = false) private UUID senderId;
    @Column(name = "recipient_id", nullable = false) private UUID recipientId;
    @Column(columnDefinition = "TEXT") private String content;
    @Column(name = "message_type", nullable = false, length = 16) private String messageType = "TEXT";
    @Column(name = "sticker_code", length = 64) private String stickerCode;
    @Column(name = "voice_data", columnDefinition = "bytea") private byte[] voiceData;
    @Column(name = "voice_mime_type", length = 96) private String voiceMimeType;
    @Column(name = "voice_duration_ms") private Integer voiceDurationMs;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "read_at") private Instant readAt;

    public ChatMessage() {}

    public ChatMessage(UUID senderId, UUID recipientId, String content, String messageType, String stickerCode) {
        this.id = UUID.randomUUID();
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.messageType = messageType;
        this.stickerCode = stickerCode;
        this.createdAt = Instant.now();
    }

    public ChatMessage(UUID senderId, UUID recipientId, byte[] voiceData, String voiceMimeType, int voiceDurationMs) {
        this(senderId, recipientId, "", "VOICE", null);
        this.voiceData = voiceData;
        this.voiceMimeType = voiceMimeType;
        this.voiceDurationMs = voiceDurationMs;
    }

    @PrePersist void prePersist() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public UUID getSenderId() { return senderId; }
    public UUID getRecipientId() { return recipientId; }
    public String getContent() { return content; }
    public String getMessageType() { return messageType; }
    public String getStickerCode() { return stickerCode; }
    public byte[] getVoiceData() { return voiceData; }
    public String getVoiceMimeType() { return voiceMimeType; }
    public Integer getVoiceDurationMs() { return voiceDurationMs; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}
