package com.fairshare.group.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record CreateGroupRequest(
        @NotBlank(message = "Group name is required")
        String name,
        String kind,
        String emoji,
        Boolean simplifyDebts,
        List<UUID> memberIds
) {}
