package com.fairshare.chat.dto;

import com.fairshare.chat.model.ChatMessage;
import com.fairshare.user.model.User;
import java.util.UUID;

public record ChatConversationResponse(
        UUID otherUserId,
        String otherUserName,
        String otherUserAvatarUrl,
        String otherUserInitials,
        ChatMessageResponse latestMessage,
        long unreadCount
) {
    public static ChatConversationResponse of(User otherUser, ChatMessage latestMessage, long unreadCount) {
        return new ChatConversationResponse(
                otherUser.getId(),
                otherUser.getName(),
                otherUser.getAvatarUrl(),
                otherUser.getInitials(),
                ChatMessageResponse.from(latestMessage),
                unreadCount
        );
    }
}
