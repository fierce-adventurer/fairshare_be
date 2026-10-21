package com.fairshare.chat.controller;

import com.fairshare.chat.dto.ChatConversationResponse;
import com.fairshare.chat.dto.ChatMessageResponse;
import com.fairshare.chat.dto.SendChatMessageRequest;
import com.fairshare.chat.service.ChatService;
import com.fairshare.shared.dto.ApiResponse;
import com.fairshare.shared.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chats")
public class ChatController {
    private final ChatService chatService;
    public ChatController(ChatService chatService) { this.chatService = chatService; }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatConversationResponse>>> conversations(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.conversations(principal.id())));
    }

    @GetMapping("/{otherUserId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> conversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID otherUserId,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "60") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.conversation(principal.id(), otherUserId, before, limit)));
    }

    @PostMapping("/{otherUserId}/messages")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> send(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID otherUserId,
            @RequestBody SendChatMessageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(chatService.send(principal.id(), otherUserId, request)));
    }

    @PostMapping(path = "/{otherUserId}/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendVoice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID otherUserId,
            @RequestPart("audio") MultipartFile audio,
            @RequestParam int durationMs
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(chatService.sendVoice(principal.id(), otherUserId, audio, durationMs)));
    }

    @GetMapping("/messages/{messageId}/voice")
    public ResponseEntity<byte[]> voice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID messageId
    ) {
        ChatService.VoiceContent voice = chatService.voice(principal.id(), messageId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(voice.mimeType()))
                .contentLength(voice.data().length)
                .body(voice.data());
    }

    @PutMapping("/{otherUserId}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID otherUserId) {
        chatService.markRead(principal.id(), otherUserId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
