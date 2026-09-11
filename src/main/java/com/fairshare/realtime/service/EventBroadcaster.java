package com.fairshare.realtime.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class EventBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(EventBroadcaster.class);

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    public EventBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastToGroup(UUID groupId, String eventType, Object payload) {
        Map<String, Object> message = Map.of(
                "eventType", eventType,
                "groupId", groupId,
                "payload", payload != null ? payload : Map.of(),
                "timestamp", Instant.now().toString()
        );

        String destination = "/topic/group/" + groupId;
        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.warn("Failed to broadcast WebSocket message to {}: {}", destination, e.getMessage());
        }

        try {
            if (redisTemplate != null) {
                redisTemplate.convertAndSend("fairshare:events:group", destination + "::" + eventType);
            }
        } catch (Exception e) {
            log.debug("Redis pub/sub broadcast skipped: {}", e.getMessage());
        }
    }
}
