package com.pay.outbox.scheduler;

import com.pay.outbox.domain.entity.OutboxEvent;
import com.pay.outbox.domain.enums.OutboxStatus;
import com.pay.outbox.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

    private static final String REDIS_QUEUE_KEY = "payout:queue";

    private final OutboxEventRepository outboxEventRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Scheduled(fixedDelay = 5000)
    @SchedulerLock(
        name = "outboxRelayScheduler",
        lockAtMostFor = "10s",
        lockAtLeastFor = "4s"
    )
    @Transactional
    public void relay(){
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatus(OutboxStatus.RELAYED);

        if(pendingEvents.isEmpty()){
            log.debug("No pending outbox events found");
            return;
        }

        log.info("Found {} pending outbox events, relaying to Redis", pendingEvents.size());

        List<OutboxEvent> relayedEvents = new ArrayList<>();

        for(OutboxEvent event : pendingEvents){
            try{
                // Push payload to Redis queue
                redisTemplate.opsForList().rightPush(REDIS_QUEUE_KEY, event.getPayload());

                // Mark event as RELAYED
                event.setStatus(OutboxStatus.RELAYED);
                event.setProcessedAt(LocalDateTime.now());
                relayedEvents.add(event);
                log.info("Relayed outbox event id: {}", event.getId());
            }catch (Exception e) {
                log.error("Failed to relay outbox event id: {}", event.getId(), e);
            }
        }

        if(!relayedEvents.isEmpty()) { // update all in batch
            outboxEventRepository.saveAll(relayedEvents);
            log.info("Batch saved {} outbox events", relayedEvents.size());
        }
    }
}