package com.fairshare.group.dto;

import com.fairshare.group.model.Group;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        String name,
        String kind,
        String emoji,
        boolean simplifyDebts,
        UUID createdBy,
        List<UUID> memberIds,
        Instant createdAt,
        Instant updatedAt
) {
    public static GroupResponse from(Group group, List<UUID> memberIds) {
        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getKind(),
                group.getEmoji(),
                group.isSimplifyDebts(),
                group.getCreatedBy(),
                memberIds,
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
}
