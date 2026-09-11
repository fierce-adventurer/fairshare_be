package com.fairshare.activity.dto;

import com.fairshare.activity.model.ActivityEvent;
import java.time.Instant;
import java.util.UUID;

public record ActivityEventResponse(
        UUID id,
        UUID groupId,
        UUID actorId,
        String eventType,
        String targetType,
        UUID targetId,
        String payload,
        Instant createdAt
) {
    public static ActivityEventResponse from(ActivityEvent event) {
        return new ActivityEventResponse(
                event.getId(),
                event.getGroupId(),
                event.getActorId(),
                event.getEventType(),
                event.getTargetType(),
                event.getTargetId(),
                event.getPayload(),
                event.getCreatedAt()
        );
    }
}
