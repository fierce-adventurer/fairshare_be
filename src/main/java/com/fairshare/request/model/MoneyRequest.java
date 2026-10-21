package com.fairshare.request.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "money_requests")
public class MoneyRequest {

    @Id
    private UUID id;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "from_user_id", nullable = false)
    private UUID fromUserId;

    @Column(name = "to_user_id")
    private UUID toUserId;

    @Column(name = "contact_id")
    private UUID contactId;

    @Column(name = "payer_name", length = 100)
    private String payerName;

    @Column(name = "payer_msisdn", length = 20)
    private String payerMsisdn;

    @Column(name = "expense_id")
    private UUID expenseId;

    @Column(name = "reminder_interval_days")
    private Integer reminderIntervalDays;

    @Column(name = "reminder_channels", nullable = false, length = 64)
    private String reminderChannels = "whatsapp";

    @Column(name = "payer_instagram_id", length = 128)
    private String payerInstagramId;

    @Column(name = "payment_mode", length = 16)
    private String paymentMode;

    @Column(name = "payment_instructions", columnDefinition = "TEXT")
    private String paymentInstructions;

    @Column(name = "payment_qr_data", columnDefinition = "TEXT")
    private String paymentQrData;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(nullable = false)
    private String description;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, length = 20)
    private String status = "open"; // open, proof_submitted, settled, expired, cancelled

    @Column(name = "settlement_transaction_id", length = 128)
    private String settlementTransactionId;

    @Column(name = "settlement_proof_data", columnDefinition = "bytea")
    private byte[] settlementProofData;

    @Column(name = "settlement_proof_mime_type", length = 96)
    private String settlementProofMimeType;

    @Column(name = "settlement_submitted_at")
    private Instant settlementSubmittedAt;

    @Column(name = "settlement_verified_at")
    private Instant settlementVerifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public MoneyRequest() {
    }

    public MoneyRequest(
            UUID id,
            UUID groupId,
            UUID fromUserId,
            UUID toUserId,
            UUID contactId,
            String payerName,
            String payerMsisdn,
            UUID expenseId,
            Integer reminderIntervalDays,
            Set<String> reminderChannels,
            String payerInstagramId,
            String paymentMode,
            String paymentInstructions,
            String paymentQrData,
            String currency,
            long amountMinor,
            String description,
            Instant expiresAt
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.groupId = groupId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.contactId = contactId;
        this.payerName = payerName;
        this.payerMsisdn = payerMsisdn;
        this.expenseId = expenseId;
        this.reminderIntervalDays = reminderIntervalDays;
        this.reminderChannels = String.join(",", new LinkedHashSet<>(reminderChannels == null || reminderChannels.isEmpty() ? Set.of("whatsapp") : reminderChannels));
        this.payerInstagramId = payerInstagramId;
        this.paymentMode = paymentMode;
        this.paymentInstructions = paymentInstructions;
        this.paymentQrData = paymentQrData;
        this.currency = currency != null ? currency : "INR";
        this.amountMinor = amountMinor;
        this.description = description;
        this.expiresAt = expiresAt;
        this.status = "open";
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getGroupId() { return groupId; }
    public void setGroupId(UUID groupId) { this.groupId = groupId; }
    public UUID getFromUserId() { return fromUserId; }
    public void setFromUserId(UUID fromUserId) { this.fromUserId = fromUserId; }
    public UUID getToUserId() { return toUserId; }
    public void setToUserId(UUID toUserId) { this.toUserId = toUserId; }
    public UUID getContactId() { return contactId; }
    public void setContactId(UUID contactId) { this.contactId = contactId; }
    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }
    public String getPayerMsisdn() { return payerMsisdn; }
    public void setPayerMsisdn(String payerMsisdn) { this.payerMsisdn = payerMsisdn; }
    public UUID getExpenseId() { return expenseId; }
    public void setExpenseId(UUID expenseId) { this.expenseId = expenseId; }
    public Integer getReminderIntervalDays() { return reminderIntervalDays; }
    public void setReminderIntervalDays(Integer reminderIntervalDays) { this.reminderIntervalDays = reminderIntervalDays; }
    public List<String> getReminderChannels() { return Arrays.stream(reminderChannels.split(",")).map(String::trim).filter(value -> !value.isBlank()).toList(); }
    public String getPayerInstagramId() { return payerInstagramId; }
    public String getPaymentMode() { return paymentMode; }
    public String getPaymentInstructions() { return paymentInstructions; }
    public String getPaymentQrData() { return paymentQrData; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public long getAmountMinor() { return amountMinor; }
    public void setAmountMinor(long amountMinor) { this.amountMinor = amountMinor; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSettlementTransactionId() { return settlementTransactionId; }
    public void setSettlementTransactionId(String settlementTransactionId) { this.settlementTransactionId = settlementTransactionId; }
    public byte[] getSettlementProofData() { return settlementProofData; }
    public void setSettlementProofData(byte[] settlementProofData) { this.settlementProofData = settlementProofData; }
    public String getSettlementProofMimeType() { return settlementProofMimeType; }
    public void setSettlementProofMimeType(String settlementProofMimeType) { this.settlementProofMimeType = settlementProofMimeType; }
    public Instant getSettlementSubmittedAt() { return settlementSubmittedAt; }
    public void setSettlementSubmittedAt(Instant settlementSubmittedAt) { this.settlementSubmittedAt = settlementSubmittedAt; }
    public Instant getSettlementVerifiedAt() { return settlementVerifiedAt; }
    public void setSettlementVerifiedAt(Instant settlementVerifiedAt) { this.settlementVerifiedAt = settlementVerifiedAt; }
    public boolean hasSettlementProof() { return settlementProofData != null && settlementProofData.length > 0; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
