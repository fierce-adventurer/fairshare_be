package com.fairshare.auth.dto;

public record AuthProvidersResponse(
        boolean phone,
        boolean google,
        boolean apple
) {}
