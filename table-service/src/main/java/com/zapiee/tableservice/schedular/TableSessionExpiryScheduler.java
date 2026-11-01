package com.zapiee.tableservice.schedular;

import com.zapiee.tableservice.enums.SessionStatus;
import com.zapiee.tableservice.model.TableSession;
import com.zapiee.tableservice.repository.TableSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TableSessionExpiryScheduler {

    private final TableSessionRepository tableSessionRepository;

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void expireCompletedSessions() {

        LocalDateTime now = LocalDateTime.now();

        List<TableSession> expiredSessions = tableSessionRepository.findExpiredSessions(
                        SessionStatus.ACTIVE, now);

        if (expiredSessions.isEmpty()) {
            return;
        }

        for (TableSession session : expiredSessions) {

            session.setStatus(SessionStatus.EXPIRED);

            log.info("Table session expired automatically. sessionId={}, restaurantId={}, tableId={}",
                    session.getId(), session.getRestaurantId(), session.getTableId());
        }

        tableSessionRepository.saveAll(expiredSessions);
    }

}