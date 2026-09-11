package com.fairshare.user.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    private String initials;

    @Column(name = "avatar_url")
    private String avatarUrl;

    private String color;

    @Column(unique = true)
    private String phone;

    @Column(name = "default_currency", nullable = false)
    private String defaultCurrency = "INR";

    @Column(nullable = false)
    private String language = "en";

    @Column(name = "onboarding_step", nullable = false)
    private String onboardingStep = "welcome";

    @Column(nullable = false)
    private String role = "user";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version = 0L;

    public User() {
    }

    public User(UUID id, String email, String passwordHash, String name) {
        this.id = id != null ? id : UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.color = "#6366F1";
        this.initials = computeInitials(name);
    }

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
        if (initials == null && name != null) initials = computeInitials(name);
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        if (name != null) initials = computeInitials(name);
    }

    public static String computeInitials(String name) {
        if (name == null || name.isBlank()) return "FS";
        return Arrays.stream(name.trim().split("\\s+"))
                .filter(s -> !s.isEmpty())
                .limit(2)
                .map(s -> String.valueOf(Character.toUpperCase(s.charAt(0))))
                .collect(Collectors.joining());
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDefaultCurrency() { return defaultCurrency; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getOnboardingStep() { return onboardingStep; }
    public void setOnboardingStep(String onboardingStep) { this.onboardingStep = onboardingStep; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
