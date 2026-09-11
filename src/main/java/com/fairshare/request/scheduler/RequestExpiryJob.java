package com.fairshare.request.scheduler;

import com.fairshare.request.repository.MoneyRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class RequestExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(RequestExpiryJob.class);

    private final MoneyRequestRepository requestRepository;

    public RequestExpiryJob(MoneyRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public int expireOldRequests() {
        Instant now = Instant.now();
        int expiredCount = requestRepository.expireOldRequests(now);
        if (expiredCount > 0) {
            log.info("RequestExpiryJob: Expired {} money requests past their deadline", expiredCount);
        }
        return expiredCount;
    }
}
