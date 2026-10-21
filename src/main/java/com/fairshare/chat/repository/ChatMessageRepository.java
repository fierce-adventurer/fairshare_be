package com.fairshare.chat.repository;

import com.fairshare.chat.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    @Query("SELECT m FROM ChatMessage m WHERE ((m.senderId = :first AND m.recipientId = :second) OR (m.senderId = :second AND m.recipientId = :first)) AND (:before IS NULL OR m.createdAt < :before) ORDER BY m.createdAt DESC")
    List<ChatMessage> findConversation(@Param("first") UUID first, @Param("second") UUID second, @Param("before") Instant before, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.senderId = :userId OR m.recipientId = :userId ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentForUser(@Param("userId") UUID userId, Pageable pageable);

    long countBySenderIdAndRecipientIdAndReadAtIsNull(UUID senderId, UUID recipientId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.readAt = :now WHERE m.senderId = :sender AND m.recipientId = :recipient AND m.readAt IS NULL")
    int markConversationRead(@Param("sender") UUID sender, @Param("recipient") UUID recipient, @Param("now") Instant now);
}
