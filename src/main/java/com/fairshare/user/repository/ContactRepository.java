package com.fairshare.user.repository;

import com.fairshare.user.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {
    List<Contact> findByOwnerIdOrderByAddedAtDesc(UUID ownerId);
    Optional<Contact> findByOwnerIdAndMsisdn(UUID ownerId, String msisdn);
    boolean existsByOwnerIdAndContactUserId(UUID ownerId, UUID contactUserId);
    void deleteByIdAndOwnerId(UUID id, UUID ownerId);
}
