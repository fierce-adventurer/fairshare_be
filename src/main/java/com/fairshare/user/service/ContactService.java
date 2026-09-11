package com.fairshare.user.service;

import com.fairshare.shared.exception.BadRequestException;
import com.fairshare.user.dto.AddContactRequest;
import com.fairshare.user.dto.ContactResponse;
import com.fairshare.user.model.Contact;
import com.fairshare.user.model.User;
import com.fairshare.user.repository.ContactRepository;
import com.fairshare.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    public ContactService(ContactRepository contactRepository, UserRepository userRepository) {
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ContactResponse> listContacts(UUID ownerId) {
        return contactRepository.findByOwnerIdOrderByAddedAtDesc(ownerId)
                .stream()
                .map(ContactResponse::from)
                .toList();
    }

    @Transactional
    public ContactResponse addContact(UUID ownerId, AddContactRequest request) {
        String msisdn = request.msisdn().trim();
        Optional<Contact> existing = contactRepository.findByOwnerIdAndMsisdn(ownerId, msisdn);
        if (existing.isPresent()) {
            throw new BadRequestException("Contact with phone number " + msisdn + " already exists.");
        }

        // Auto-resolve to registered user if phone matches
        UUID contactUserId = userRepository.findByPhone(msisdn)
                .map(User::getId)
                .orElse(null);

        Contact contact = new Contact(ownerId, contactUserId, request.name().trim(), msisdn);
        Contact saved = contactRepository.save(contact);
        return ContactResponse.from(saved);
    }

    @Transactional
    public void deleteContact(UUID ownerId, UUID contactId) {
        contactRepository.deleteByIdAndOwnerId(contactId, ownerId);
    }
}
