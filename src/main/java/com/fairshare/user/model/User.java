package com.fairshare.user.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
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

    @Column(name = "first_name", length = 80)
    private String firstName;

    @Column(name = "last_name", length = 80)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String initials;

    @Column(name = "avatar_url")
    private String avatarUrl;

    private String color;

    @Column(unique = true)
    private String phone;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Column(name = "profile_reviewed_at")
    private Instant profileReviewedAt;

    @Column(name = "instagram_handle", length = 128)
    private String instagramHandle;

    @Column(name = "upi_id", length = 128)
    private String upiId;

    @Column(name = "bank_account_name", length = 120)
    private String bankAccountName;

    @Column(name = "bank_account_number", length = 64)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc", length = 32)
    private String bankIfsc;

    @Column(name = "payment_qr_data", columnDefinition = "TEXT")
    private String paymentQrData;

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
        applyNameParts(name);
        this.color = "#6366F1";
        this.initials = computeInitials(name);
    }

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
        if (initials == null && name != null) initials = computeInitials(name);
        if (firstName == null && name != null) applyNameParts(name);
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

    public void applyNameParts(String fullName) {
        String normalized = fullName == null ? "" : fullName.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) return;
        int separator = normalized.indexOf(' ');
        this.firstName = separator < 0 ? normalized : normalized.substring(0, separator);
        this.lastName = separator < 0 ? "" : normalized.substring(separator + 1);
        this.name = normalized;
    }

    public void setNameParts(String firstName, String lastName) {
        this.firstName = firstName == null ? "" : firstName.trim();
        this.lastName = lastName == null ? "" : lastName.trim();
        this.name = (this.firstName + " " + this.lastName).trim();
        this.initials = computeInitials(this.name);
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getName() { return name; }
    public void setName(String name) { applyNameParts(name); }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getInitials() { return initials; }
    public void setInitials(String initials) { this.initials = initials; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Instant getPhoneVerifiedAt() { return phoneVerifiedAt; }
    public void setPhoneVerifiedAt(Instant phoneVerifiedAt) { this.phoneVerifiedAt = phoneVerifiedAt; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(Instant emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }
    public Instant getTermsAcceptedAt() { return termsAcceptedAt; }
    public void setTermsAcceptedAt(Instant termsAcceptedAt) { this.termsAcceptedAt = termsAcceptedAt; }
    public Instant getProfileReviewedAt() { return profileReviewedAt; }
    public void setProfileReviewedAt(Instant profileReviewedAt) { this.profileReviewedAt = profileReviewedAt; }
    public String getInstagramHandle() { return instagramHandle; }
    public void setInstagramHandle(String instagramHandle) { this.instagramHandle = instagramHandle; }
    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }
    public String getBankAccountName() { return bankAccountName; }
    public void setBankAccountName(String bankAccountName) { this.bankAccountName = bankAccountName; }
    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }
    public String getBankIfsc() { return bankIfsc; }
    public void setBankIfsc(String bankIfsc) { this.bankIfsc = bankIfsc; }
    public String getPaymentQrData() { return paymentQrData; }
    public void setPaymentQrData(String paymentQrData) { this.paymentQrData = paymentQrData; }
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
