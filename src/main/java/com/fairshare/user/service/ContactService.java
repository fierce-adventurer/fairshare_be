package com.fairshare.user.service;

import com.fairshare.shared.exception.BadRequestException;
import com.fairshare.user.dto.AddContactRequest;
import com.fairshare.user.dto.ContactResponse;
import com.fairshare.user.dto.UpdateContactRequest;
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
        String msisdn = normalizeMsisdn(request.msisdn());
        if (msisdn.length() < 11 || msisdn.length() > 16) {
            throw new BadRequestException("Enter a valid phone number including its country code.");
        }
        UUID contactUserId = userRepository.findByPhone(msisdn)
                .map(User::getId)
                .orElse(null);
        Optional<Contact> existing = contactRepository.findByOwnerIdAndMsisdn(ownerId, msisdn)
                .or(() -> contactRepository.findByOwnerIdOrderByAddedAtDesc(ownerId).stream()
                        .filter(contact -> normalizeMsisdn(contact.getMsisdn()).equals(msisdn))
                        .findFirst());
        if (existing.isPresent()) {
            Contact contact = existing.get();
            contact.setName(request.name().trim());
            contact.setMsisdn(msisdn);
            if (contactUserId != null) contact.setContactUserId(contactUserId);
            return ContactResponse.from(contactRepository.save(contact));
        }

        Contact contact = new Contact(ownerId, contactUserId, request.name().trim(), msisdn);
        Contact saved = contactRepository.save(contact);
        return ContactResponse.from(saved);
    }

    @Transactional
    public ContactResponse updateContact(UUID ownerId, UUID contactId, UpdateContactRequest request) {
        Contact contact = contactRepository.findById(contactId)
                .filter(entry -> entry.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new BadRequestException("Contact not found."));
        if (request.name() != null && !request.name().isBlank()) contact.setName(request.name().trim());
        if (request.favorite() != null) contact.setFavorite(request.favorite());
        return ContactResponse.from(contactRepository.save(contact));
    }

    @Transactional
    public void deleteContact(UUID ownerId, UUID contactId) {
        contactRepository.deleteByIdAndOwnerId(contactId, ownerId);
    }

    public static String normalizeMsisdn(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.length() == 10) digits = "91" + digits;
        return digits.isBlank() ? "" : "+" + digits;
    }
}
