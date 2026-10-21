package com.fairshare.chat.service;

import com.fairshare.chat.dto.ChatConversationResponse;
import com.fairshare.chat.dto.ChatMessageResponse;
import com.fairshare.chat.dto.SendChatMessageRequest;
import com.fairshare.chat.model.ChatMessage;
import com.fairshare.chat.repository.ChatMessageRepository;
import com.fairshare.realtime.service.EventBroadcaster;
import com.fairshare.shared.exception.BadRequestException;
import com.fairshare.shared.exception.ResourceNotFoundException;
import com.fairshare.shared.exception.UnauthorizedException;
import com.fairshare.user.repository.ContactRepository;
import com.fairshare.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

@Service
public class ChatService {
    private static final long MAX_VOICE_BYTES = 5L * 1024L * 1024L;
    private final ChatMessageRepository messages;
    private final ContactRepository contacts;
    private final UserRepository users;
    private final EventBroadcaster broadcaster;

    public ChatService(ChatMessageRepository messages, ContactRepository contacts, UserRepository users, EventBroadcaster broadcaster) {
        this.messages = messages;
        this.contacts = contacts;
        this.users = users;
        this.broadcaster = broadcaster;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> conversation(UUID userId, UUID otherUserId, Instant before, int requestedLimit) {
        ensureCanChat(userId, otherUserId);
        int limit = Math.max(1, Math.min(requestedLimit, 100));
        List<ChatMessage> page = messages.findConversation(userId, otherUserId, before, PageRequest.of(0, limit));
        Collections.reverse(page);
        return page.stream().map(ChatMessageResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatConversationResponse> conversations(UUID userId) {
        List<ChatMessage> recent = messages.findRecentForUser(userId, PageRequest.of(0, 1000));
        Map<UUID, ChatMessage> latestByPartner = new LinkedHashMap<>();
        for (ChatMessage message : recent) {
            UUID partnerId = message.getSenderId().equals(userId) ? message.getRecipientId() : message.getSenderId();
            latestByPartner.putIfAbsent(partnerId, message);
        }
        return latestByPartner.entrySet().stream().map(entry -> {
            var partner = users.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Chat participant not found."));
            long unread = messages.countBySenderIdAndRecipientIdAndReadAtIsNull(entry.getKey(), userId);
            return ChatConversationResponse.of(partner, entry.getValue(), unread);
        }).toList();
    }

    @Transactional
    public ChatMessageResponse send(UUID senderId, UUID recipientId, SendChatMessageRequest request) {
        ensureCanChat(senderId, recipientId);
        String type = request.messageType() == null ? "TEXT" : request.messageType().trim().toUpperCase(Locale.ROOT);
        if (!List.of("TEXT", "EMOJI", "STICKER").contains(type)) throw new BadRequestException("Unsupported message type.");
        String content = request.content() == null ? "" : request.content().trim();
        String sticker = request.stickerCode() == null ? null : request.stickerCode().trim();
        if (content.isBlank() && (sticker == null || sticker.isBlank())) throw new BadRequestException("Message cannot be empty.");
        if (content.length() > 2000) throw new BadRequestException("Message is too long.");
        ChatMessageResponse response = ChatMessageResponse.from(messages.save(new ChatMessage(senderId, recipientId, content, type, sticker)));
        broadcaster.broadcastToUser(recipientId, "chat.message", response);
        return response;
    }

    @Transactional
    public ChatMessageResponse sendVoice(UUID senderId, UUID recipientId, MultipartFile audio, int durationMs) {
        ensureCanChat(senderId, recipientId);
        if (audio == null || audio.isEmpty()) throw new BadRequestException("Voice message cannot be empty.");
        if (audio.getSize() > MAX_VOICE_BYTES) throw new BadRequestException("Voice messages must be smaller than 5 MB.");
        if (durationMs < 250 || durationMs > 60_000) throw new BadRequestException("Voice messages must be between 0.25 and 60 seconds.");
        String mimeType = audio.getContentType() == null ? "audio/webm" : audio.getContentType().toLowerCase(Locale.ROOT);
        if (!mimeType.startsWith("audio/")) throw new BadRequestException("The uploaded file is not audio.");
        try {
            ChatMessage message = messages.save(new ChatMessage(senderId, recipientId, audio.getBytes(), mimeType, durationMs));
            ChatMessageResponse response = ChatMessageResponse.from(message);
            broadcaster.broadcastToUser(recipientId, "chat.message", response);
            return response;
        } catch (IOException exception) {
            throw new BadRequestException("The voice message could not be read.");
        }
    }

    @Transactional(readOnly = true)
    public VoiceContent voice(UUID userId, UUID messageId) {
        ChatMessage message = messages.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Voice message not found."));
        if (!message.getSenderId().equals(userId) && !message.getRecipientId().equals(userId)) {
            throw new UnauthorizedException("You do not have access to this voice message.");
        }
        if (message.getVoiceData() == null || message.getVoiceData().length == 0) {
            throw new ResourceNotFoundException("Voice message audio is unavailable.");
        }
        return new VoiceContent(message.getVoiceData(),
                message.getVoiceMimeType() == null ? "audio/webm" : message.getVoiceMimeType());
    }

    @Transactional
    public void markRead(UUID userId, UUID otherUserId) {
        ensureCanChat(userId, otherUserId);
        messages.markConversationRead(otherUserId, userId, Instant.now());
    }

    private void ensureCanChat(UUID userId, UUID otherUserId) {
        if (userId.equals(otherUserId)) throw new BadRequestException("You cannot start a chat with yourself.");
        if (!users.existsById(otherUserId)) throw new BadRequestException("That Fairshare user no longer exists.");
        if (!contacts.existsByOwnerIdAndContactUserId(userId, otherUserId)) {
            throw new UnauthorizedException("Add this Fairshare user to your contacts before starting a chat.");
        }
    }

    public record VoiceContent(byte[] data, String mimeType) {}
}
