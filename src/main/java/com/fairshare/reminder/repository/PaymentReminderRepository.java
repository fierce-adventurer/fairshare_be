package com.fairshare.reminder.repository;
import com.fairshare.reminder.model.PaymentReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
public interface PaymentReminderRepository extends JpaRepository<PaymentReminder,UUID>{
    List<PaymentReminder> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    List<PaymentReminder> findByOwnerIdAndSourceTypeAndSourceIdOrderByCreatedAtDesc(UUID ownerId,String sourceType,UUID sourceId);
    List<PaymentReminder> findByStatusAndNextSendAtLessThanEqual(String status, Instant now);
}
