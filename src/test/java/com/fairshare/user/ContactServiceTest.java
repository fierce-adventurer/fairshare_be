package com.fairshare.user;

import com.fairshare.user.dto.AddContactRequest;
import com.fairshare.user.dto.ContactResponse;
import com.fairshare.user.model.Contact;
import com.fairshare.user.model.User;
import com.fairshare.user.repository.ContactRepository;
import com.fairshare.user.repository.UserRepository;
import com.fairshare.user.service.ContactService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {
    @Mock ContactRepository contacts;
    @Mock UserRepository users;

    @Test
    void reusesLegacyFormattedNumberAndLinksRegisteredUser() {
        UUID ownerId = UUID.randomUUID();
        UUID registeredId = UUID.randomUUID();
        Contact legacy = new Contact(ownerId, null, "Old name", "98765 43210");
        User registered = new User(registeredId, "payer@example.test", null, "Aanya Sharma");
        registered.setPhone("+919876543210");
        when(contacts.findByOwnerIdAndMsisdn(ownerId, "+919876543210")).thenReturn(Optional.empty());
        when(contacts.findByOwnerIdOrderByAddedAtDesc(ownerId)).thenReturn(List.of(legacy));
        when(users.findByPhone("+919876543210")).thenReturn(Optional.of(registered));
        when(contacts.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContactResponse response = new ContactService(contacts, users)
                .addContact(ownerId, new AddContactRequest("Aanya Sharma", "+91 98765-43210"));

        assertEquals(legacy.getId(), response.id());
        assertEquals("+919876543210", response.msisdn());
        assertEquals(registeredId, response.contactUserId());
        assertTrue(response.isRegisteredUser());
        verify(contacts, times(1)).save(legacy);
    }
}
