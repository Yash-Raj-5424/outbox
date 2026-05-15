package com.pay.outbox.scheduler;

import com.pay.outbox.domain.entity.OutboxEvent;
import com.pay.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private static final String REDIS_QUEUE_KEY = "payout:queue";

    private final OutboxEventRepository outboxEventRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void relay() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatus("PENDING");

        if (pendingEvents.isEmpty()) {
            log.debug("No pending outbox events found");
            return;
        }

        log.info("Found {} pending outbox events, relaying to Redis", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                // Push payload to Redis queue
                redisTemplate.opsForList().rightPush(REDIS_QUEUE_KEY, event.getPayload());

                // Mark event as RELAYED
                event.setStatus("RELAYED");
                event.setProcessedAt(LocalDateTime.now());
                outboxEventRepository.save(event);

                log.info("Relayed outbox event id: {}", event.getId());
            } catch (Exception e) {
                log.error("Failed to relay outbox event id: {}", event.getId(), e);
            }
        }
    }
}