package com.fairshare.reminder.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "payment_reminders")
public class PaymentReminder {
    @Id private UUID id;
    @Column(name = "owner_id", nullable = false) private UUID ownerId;
    @Column(name = "source_type", nullable = false, length = 16) private String sourceType;
    @Column(name = "source_id", nullable = false) private UUID sourceId;
    @Column(name = "expense_description", nullable = false) private String expenseDescription;
    @Column(nullable = false, length = 3) private String currency = "INR";
    @Column(name = "amount_minor", nullable = false) private long amountMinor;
    @Column(name = "payer_user_id") private UUID payerUserId;
    @Column(name = "payer_name", nullable = false) private String payerName;
    @Column(name = "payer_msisdn", length = 20) private String payerMsisdn;
    @Column(name = "payer_instagram_id", length = 128) private String payerInstagramId;
    @Column(name = "payment_mode", length = 16) private String paymentMode;
    @Column(name = "payment_instructions", columnDefinition = "TEXT") private String paymentInstructions;
    @Column(nullable = false, length = 64) private String channels = "whatsapp";
    @Column(name = "interval_days", nullable = false) private int intervalDays;
    @Column(nullable = false, length = 16) private String status = "active";
    @Column(name = "next_send_at", nullable = false) private Instant nextSendAt;
    @Column(name = "last_sent_at") private Instant lastSentAt;
    @Column(name = "last_delivery_status", length = 32) private String lastDeliveryStatus;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public PaymentReminder() {}
    public PaymentReminder(UUID ownerId, String sourceType, UUID sourceId, String description, String currency,
                           long amountMinor, UUID payerUserId, String payerName, String payerMsisdn, int intervalDays) {
        this(ownerId, sourceType, sourceId, description, currency, amountMinor, payerUserId, payerName, payerMsisdn,
                null, Set.of("whatsapp"), intervalDays, null, null);
    }
    public PaymentReminder(UUID ownerId, String sourceType, UUID sourceId, String description, String currency,
                           long amountMinor, UUID payerUserId, String payerName, String payerMsisdn,
                           String payerInstagramId, Set<String> channels, int intervalDays) {
        this(ownerId, sourceType, sourceId, description, currency, amountMinor, payerUserId, payerName, payerMsisdn,
                payerInstagramId, channels, intervalDays, null, null);
    }
    public PaymentReminder(UUID ownerId, String sourceType, UUID sourceId, String description, String currency,
                           long amountMinor, UUID payerUserId, String payerName, String payerMsisdn,
                           String payerInstagramId, Set<String> channels, int intervalDays,
                           String paymentMode, String paymentInstructions) {
        this.id = UUID.randomUUID(); this.ownerId = ownerId; this.sourceType = sourceType; this.sourceId = sourceId;
        this.expenseDescription = description; this.currency = currency; this.amountMinor = amountMinor;
        this.payerUserId = payerUserId; this.payerName = payerName; this.payerMsisdn = payerMsisdn;
        this.payerInstagramId = payerInstagramId;
        this.paymentMode = paymentMode;
        this.paymentInstructions = paymentInstructions;
        this.channels = String.join(",", new LinkedHashSet<>(channels));
        this.intervalDays = intervalDays; this.nextSendAt = Instant.now().plus(intervalDays, ChronoUnit.DAYS);
        this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    @PrePersist void prePersist(){ if(id==null)id=UUID.randomUUID(); if(createdAt==null)createdAt=Instant.now(); if(updatedAt==null)updatedAt=createdAt; }
    @PreUpdate void preUpdate(){ updatedAt=Instant.now(); }
    public UUID getId(){return id;} public UUID getOwnerId(){return ownerId;} public String getSourceType(){return sourceType;}
    public UUID getSourceId(){return sourceId;} public String getExpenseDescription(){return expenseDescription;}
    public String getCurrency(){return currency;} public long getAmountMinor(){return amountMinor;} public UUID getPayerUserId(){return payerUserId;}
    public String getPayerName(){return payerName;} public String getPayerMsisdn(){return payerMsisdn;}
    public String getPayerInstagramId(){return payerInstagramId;}
    public String getPaymentMode(){return paymentMode;}
    public String getPaymentInstructions(){return paymentInstructions;}
    public List<String> getChannels(){return Arrays.stream(channels.split(",")).map(String::trim).filter(value->!value.isBlank()).toList();}
    public int getIntervalDays(){return intervalDays;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;} public Instant getNextSendAt(){return nextSendAt;}
    public void setNextSendAt(Instant nextSendAt){this.nextSendAt=nextSendAt;} public Instant getLastSentAt(){return lastSentAt;}
    public void setLastSentAt(Instant lastSentAt){this.lastSentAt=lastSentAt;} public String getLastDeliveryStatus(){return lastDeliveryStatus;}
    public void setLastDeliveryStatus(String value){this.lastDeliveryStatus=value;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
