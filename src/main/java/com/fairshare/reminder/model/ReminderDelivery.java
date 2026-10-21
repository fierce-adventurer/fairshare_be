package com.fairshare.reminder.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reminder_deliveries")
public class ReminderDelivery {
    @Id private UUID id;
    @Column(name="reminder_id",nullable=false) private UUID reminderId;
    @Column(nullable=false,length=24) private String channel;
    @Column(length=64) private String destination;
    @Column(nullable=false,length=32) private String status;
    @Column(name="provider_reference") private String providerReference;
    @Column(name="error_message",length=512) private String errorMessage;
    @Column(name="attempted_at",nullable=false) private Instant attemptedAt;
    public ReminderDelivery(){}
    public ReminderDelivery(UUID reminderId,String channel,String destination,String status,String providerReference,String errorMessage){
        this.id=UUID.randomUUID();this.reminderId=reminderId;this.channel=channel;this.destination=destination;
        this.status=status;this.providerReference=providerReference;this.errorMessage=errorMessage;this.attemptedAt=Instant.now();
    }
    @PrePersist void prePersist(){if(id==null)id=UUID.randomUUID();if(attemptedAt==null)attemptedAt=Instant.now();}
}
