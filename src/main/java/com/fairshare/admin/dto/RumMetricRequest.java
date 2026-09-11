package com.fairshare.admin.dto;

import java.util.Map;

public record RumMetricRequest(
        String eventName,
        String path,
        Long durationMs,
        Map<String, Object> metadata
) {}
