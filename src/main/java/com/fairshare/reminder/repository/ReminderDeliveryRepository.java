package com.fairshare.reminder.repository;
import com.fairshare.reminder.model.ReminderDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ReminderDeliveryRepository extends JpaRepository<ReminderDelivery,UUID>{}
