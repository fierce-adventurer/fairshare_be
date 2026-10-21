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

    @Column(nullable = false)
    private boolean favorite = false;

    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Contact() {
    }

    public Contact(UUID ownerId, UUID contactUserId, String name, String msisdn) {
        this.id = UUID.randomUUID();
        this.ownerId = ownerId;
        this.contactUserId = contactUserId;
        this.name = name;
        this.msisdn = msisdn;
        this.addedAt = Instant.now();
        this.updatedAt = this.addedAt;
    }

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (addedAt == null) addedAt = Instant.now();
        if (updatedAt == null) updatedAt = addedAt;
    }

    @PreUpdate
    public void preUpdate() { updatedAt = Instant.now(); }

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
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
