package com.fairshare.chat;

import com.fairshare.chat.dto.ChatMessageResponse;
import com.fairshare.chat.dto.SendChatMessageRequest;
import com.fairshare.chat.model.ChatMessage;
import com.fairshare.chat.repository.ChatMessageRepository;
import com.fairshare.chat.service.ChatService;
import com.fairshare.realtime.service.EventBroadcaster;
import com.fairshare.shared.exception.UnauthorizedException;
import com.fairshare.user.model.User;
import com.fairshare.user.repository.ContactRepository;
import com.fairshare.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {
    @Mock ChatMessageRepository messages;
    @Mock ContactRepository contacts;
    @Mock UserRepository users;
    @Mock EventBroadcaster broadcaster;

    @Test
    void sendsMessageOnlyToLinkedFairshareContactAndBroadcastsIt() {
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        when(users.existsById(recipient)).thenReturn(true);
        when(contacts.existsByOwnerIdAndContactUserId(sender, recipient)).thenReturn(true);
        when(messages.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ChatService service = new ChatService(messages, contacts, users, broadcaster);

        ChatMessageResponse response = service.send(sender, recipient, new SendChatMessageRequest("Hello 👋", "EMOJI", null));

        assertEquals("Hello 👋", response.content());
        assertEquals("EMOJI", response.messageType());
        verify(broadcaster).broadcastToUser(eq(recipient), eq("chat.message"), any(ChatMessageResponse.class));
    }

    @Test
    void storesVoiceMessageMetadataAndProtectsAudioFromOtherUsers() throws Exception {
        UUID sender = UUID.randomUUID();
        UUID recipient = UUID.randomUUID();
        when(users.existsById(recipient)).thenReturn(true);
        when(contacts.existsByOwnerIdAndContactUserId(sender, recipient)).thenReturn(true);
        when(messages.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ChatService service = new ChatService(messages, contacts, users, broadcaster);
        MockMultipartFile audio = new MockMultipartFile(
                "audio", "voice.webm", "audio/webm", "voice-data".getBytes(StandardCharsets.UTF_8));

        ChatMessageResponse response = service.sendVoice(sender, recipient, audio, 1_850);

        assertEquals("VOICE", response.messageType());
        assertEquals(1_850, response.voiceDurationMs());
        assertTrue(response.hasVoice());
        verify(broadcaster).broadcastToUser(eq(recipient), eq("chat.message"), any(ChatMessageResponse.class));

        ChatMessage persisted = new ChatMessage(sender, recipient, audio.getBytes(), "audio/webm", 1_850);
        when(messages.findById(persisted.getId())).thenReturn(Optional.of(persisted));
        assertThrows(UnauthorizedException.class, () -> service.voice(UUID.randomUUID(), persisted.getId()));
        assertArrayEquals(audio.getBytes(), service.voice(sender, persisted.getId()).data());
    }

    @Test
    void buildsConversationInboxWithUnreadCount() {
        UUID currentUser = UUID.randomUUID();
        UUID partnerId = UUID.randomUUID();
        User partner = new User(partnerId, "friend@fairshare.local", "hash", "Maya Shah");
        ChatMessage latest = new ChatMessage(partnerId, currentUser, "Ready to settle?", "TEXT", null);
        when(messages.findRecentForUser(eq(currentUser), any())).thenReturn(List.of(latest));
        when(users.findById(partnerId)).thenReturn(Optional.of(partner));
        when(messages.countBySenderIdAndRecipientIdAndReadAtIsNull(partnerId, currentUser)).thenReturn(2L);
        ChatService service = new ChatService(messages, contacts, users, broadcaster);

        var inbox = service.conversations(currentUser);

        assertEquals(1, inbox.size());
        assertEquals("Maya Shah", inbox.getFirst().otherUserName());
        assertEquals(2L, inbox.getFirst().unreadCount());
        assertEquals("Ready to settle?", inbox.getFirst().latestMessage().content());
    }
}
