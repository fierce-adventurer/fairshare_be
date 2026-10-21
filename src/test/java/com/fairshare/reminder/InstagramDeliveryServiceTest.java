package com.fairshare.reminder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fairshare.reminder.service.InstagramDeliveryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstagramDeliveryServiceTest {
    private final InstagramDeliveryService service = new InstagramDeliveryService(new ObjectMapper(), "", "", "v25.0");

    @Test
    void requiresARecipientConversationBeforeSending() {
        assertEquals("recipient_link_required", service.send(null, "Reminder").status());
    }

    @Test
    void queuesSafelyWhenProfessionalAccountCredentialsAreMissing() {
        assertEquals("queued_configuration_required", service.send("ig-scoped-recipient", "Reminder").status());
    }
}
