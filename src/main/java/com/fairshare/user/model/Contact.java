package com.fairshare.user.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contacts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_owner_msisdn", columnNames = {"owner_id", "msisdn"})
})
public class Contact {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "contact_user_id")
    private UUID contactUserId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String msisdn;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    public Contact() {
    }

    public Contact(UUID ownerId, UUID contactUserId, String name, String msisdn) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.contactUserId = contactUserId;
        this.name = name;
        this.msisdn = msisdn;
        this.addedAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (addedAt == null) addedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }
    public UUID getContactUserId() { return contactUserId; }
    public void setContactUserId(UUID contactUserId) { this.contactUserId = contactUserId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMsisdn() { return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }
    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }
}
