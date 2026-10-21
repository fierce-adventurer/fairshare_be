package com.fairshare.reminder;

import com.fairshare.reminder.dto.CreateReminderRequest;
import com.fairshare.reminder.dto.ReminderResponse;
import com.fairshare.reminder.model.PaymentReminder;
import com.fairshare.reminder.repository.PaymentReminderRepository;
import com.fairshare.reminder.repository.ReminderDeliveryRepository;
import com.fairshare.reminder.service.ReminderService;
import com.fairshare.reminder.service.WhatsAppDeliveryService;
import com.fairshare.reminder.service.InstagramDeliveryService;
import com.fairshare.user.model.User;
import com.fairshare.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {
    @Mock PaymentReminderRepository reminders;
    @Mock ReminderDeliveryRepository deliveries;
    @Mock UserRepository users;
    @Mock WhatsAppDeliveryService whatsApp;
    @Mock InstagramDeliveryService instagram;

    @Test
    void createsRecurringReminderAndAuditsServerSideDeliveryAttempt() {
        UUID ownerId = UUID.randomUUID();
        UUID payerId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        User payer = new User(payerId, "payer@example.test", null, "Kabir Rao");
        payer.setPhone("98765 43210");
        when(users.findById(payerId)).thenReturn(Optional.of(payer));
        when(reminders.save(any(PaymentReminder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReminderService service = new ReminderService(reminders, deliveries, users, whatsApp, instagram);

        ReminderResponse created = service.create(ownerId, new CreateReminderRequest("expense", expenseId, "Dinner", "INR", 12500, payerId, null, null, null, java.util.Set.of("whatsapp"), 10));
        assertEquals(10, created.intervalDays());
        assertEquals("+919876543210", created.payerMsisdn());

        PaymentReminder entity = new PaymentReminder(ownerId, "expense", expenseId, "Dinner", "INR", 12500, payerId, "Kabir Rao", "+919876543210", 10);
        when(reminders.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(whatsApp.send(anyString(), anyString())).thenReturn(new WhatsAppDeliveryService.DeliveryResult("queued_configuration_required", null, "not configured"));

        ReminderResponse delivered = service.sendNow(ownerId, entity.getId());
        assertEquals("queued_configuration_required", delivered.lastDeliveryStatus());
        assertNotNull(delivered.lastSentAt());
        verify(deliveries).save(any());
    }
}
