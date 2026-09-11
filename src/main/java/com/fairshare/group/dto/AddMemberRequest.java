package com.fairshare.group.dto;

import java.util.UUID;

public record AddMemberRequest(
        UUID userId,
        String msisdn
) {}
