package com.fairshare.user.controller;

import com.fairshare.shared.dto.ApiResponse;
import com.fairshare.shared.security.UserPrincipal;
import com.fairshare.user.dto.AddContactRequest;
import com.fairshare.user.dto.ContactResponse;
import com.fairshare.user.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/contacts")
@Tag(name = "Contacts", description = "Phone number (MSISDN) contact management")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    @Operation(summary = "List all contacts for current user")
    public ResponseEntity<ApiResponse<List<ContactResponse>>> listContacts(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ContactResponse> contacts = contactService.listContacts(principal.id());
        return ResponseEntity.ok(ApiResponse.ok(contacts));
    }

    @PostMapping
    @Operation(summary = "Add a contact by phone number (MSISDN)")
    public ResponseEntity<ApiResponse<ContactResponse>> addContact(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddContactRequest request
    ) {
        ContactResponse contact = contactService.addContact(principal.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(contact, "Contact added successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a contact by ID")
    public ResponseEntity<ApiResponse<Void>> deleteContact(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        contactService.deleteContact(principal.id(), id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Contact removed"));
    }
}
