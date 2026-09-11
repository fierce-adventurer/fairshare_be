package com.fairshare.billing.scheduler;

import com.fairshare.billing.repository.UpcomingBillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Component
public class BillDueScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillDueScheduler.class);

    private final UpcomingBillRepository billRepository;

    public BillDueScheduler(UpcomingBillRepository billRepository) {
        this.billRepository = billRepository;
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public int markOverdueBills() {
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();
        int updatedCount = billRepository.markOverdueBills(today, now);
        if (updatedCount > 0) {
            log.info("BillDueScheduler: Marked {} bills as overdue on {}", updatedCount, today);
        }
        return updatedCount;
    }
}
