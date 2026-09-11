package com.fairshare.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AddContactRequest(
        @NotBlank(message = "Contact name is required")
        String name,
        @NotBlank(message = "Phone number is required")
        String msisdn
) {}
