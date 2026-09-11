package com.fairshare.user.dto;

import com.fairshare.user.model.Contact;
import java.time.Instant;
import java.util.UUID;

public record ContactResponse(
        UUID id,
        UUID ownerId,
        UUID contactUserId,
        String name,
        String msisdn,
        boolean isRegisteredUser,
        Instant addedAt
) {
    public static ContactResponse from(Contact contact) {
        return new ContactResponse(
                contact.getId(),
                contact.getOwnerId(),
                contact.getContactUserId(),
                contact.getName(),
                contact.getMsisdn(),
                contact.getContactUserId() != null,
                contact.getAddedAt()
        );
    }
}
