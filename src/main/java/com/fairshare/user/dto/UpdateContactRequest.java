package com.fairshare.user.dto;

public record UpdateContactRequest(
        String name,
        Boolean favorite
) {}
