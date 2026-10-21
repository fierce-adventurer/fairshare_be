package com.fairshare.request;

import com.fairshare.reminder.service.ReminderService;
import com.fairshare.request.dto.CreateMoneyRequestRequest;
import com.fairshare.request.model.MoneyRequest;
import com.fairshare.request.repository.MoneyRequestRepository;
import com.fairshare.request.service.MoneyRequestService;
import com.fairshare.user.model.User;
import com.fairshare.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MoneyRequestServiceTest {
    @Mock MoneyRequestRepository requests;
    @Mock ReminderService reminders;
    @Mock UserRepository users;

    @Test
    void snapshotsSelectedUpiDestinationWhenRequestIsCreated() {
        UUID senderId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        User sender = new User(senderId, "sender@example.test", "hash", "Sender User");
        sender.setUpiId("sender@upi");
        when(users.findById(senderId)).thenReturn(Optional.of(sender));
        when(requests.save(any(MoneyRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MoneyRequestService service = new MoneyRequestService(requests, reminders, users);

        var response = service.createRequest(senderId, new CreateMoneyRequestRequest(
                null, payerId, null, "Payer", "+919876543210", null, null,
                Set.of("whatsapp"), null, "UPI", "INR", 12500, "Dinner", Instant.now().plusSeconds(3600)));

        assertEquals("UPI", response.paymentMode());
        assertEquals("UPI ID: sender@upi", response.paymentInstructions());
        assertNull(response.paymentQrData());
    }

    @Test
    void payerSubmitsScreenshotAndRequesterMustVerifyIt() {
        UUID senderId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        MoneyRequest request = new MoneyRequest(UUID.randomUUID(), null, senderId, payerId, null,
                "Payer", "+919876543210", null, 7, Set.of("whatsapp"), null,
                "UPI", "UPI ID: sender@upi", null, "INR", 12500, "Dinner", Instant.now().plusSeconds(3600));
        when(requests.findById(request.getId())).thenReturn(Optional.of(request));
        when(requests.save(any(MoneyRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MoneyRequestService service = new MoneyRequestService(requests, reminders, users);
        MockMultipartFile proof = new MockMultipartFile("proof", "paid.png", "image/png",
                "not-a-real-image-but-valid-upload-bytes".getBytes(StandardCharsets.UTF_8));

        var submitted = service.submitSettlementProof(payerId, request.getId(), "TXN-123456", proof);

        assertEquals("proof_submitted", submitted.status());
        assertEquals("TXN-123456", submitted.settlementTransactionId());
        assertTrue(submitted.hasSettlementProof());
        assertNotNull(submitted.settlementSubmittedAt());
        verify(reminders).changeStatusForSource(senderId, "request", request.getId(), "paused");

        var verified = service.verifySettlement(senderId, request.getId());
        assertEquals("settled", verified.status());
        assertNotNull(verified.settlementVerifiedAt());
        verify(reminders).changeStatusForSource(senderId, "request", request.getId(), "settled");
    }
}
