package com.zapiee.mediaservice.util;

import com.zapiee.mediaservice.repository.MediaIdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyCleanupJob {

    private final MediaIdempotencyKeyRepository repository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpiredKeys() {
        int deleted = repository.deleteExpired(Instant.now());
        if (deleted > 0) {
            log.info("Purged {} expired media idempotency keys", deleted);
        }
    }
}
