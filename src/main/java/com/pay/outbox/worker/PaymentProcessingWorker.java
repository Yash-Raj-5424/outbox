package com.pay.outbox.worker;

import com.pay.outbox.domain.entity.Payout;
import com.pay.outbox.domain.enums.PayoutStatus;
import com.pay.outbox.repository.PayoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessingWorker {

    private static final String REDIS_QUEUE_KEY = "payout:queue";
    private final RedisTemplate<String, String> redisTemplate;
    private final PayoutRepository payoutRepository;
    private final Random random = new Random();

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void process() {
        // Pop from Redis queue (blocking left pop)
        String payload = redisTemplate.opsForList().leftPop(REDIS_QUEUE_KEY);

        if (payload == null) {
            log.debug("No events in Redis queue");
            return;
        }

        log.info("Processing payload from queue: {}", payload);

        try {
            UUID payoutId = extractPayoutId(payload);
            Optional<Payout> optionalPayout = payoutRepository.findById(payoutId);

            if (optionalPayout.isEmpty()) {
                log.warn("Payout not found for id: {}", payoutId);
                return;
            }

            Payout payout = optionalPayout.get();
            payout.setAttempts(payout.getAttempts() + 1);

            // Simulate payment gateway response
            PayoutStatus result = simulatePaymentGateway();
            payout.setStatus(result);
            payoutRepository.save(payout);

            log.info("Payout id: {} processed with status: {}", payoutId, result);

        } catch (Exception e) {
            log.error("Error processing payment from queue", e);
        }
    }

    private PayoutStatus simulatePaymentGateway() {
        int outcome = random.nextInt(100);
        if (outcome < 70) return PayoutStatus.COMPLETED;
        if (outcome < 90) return PayoutStatus.FAILED;
        return PayoutStatus.CANCELLED;
    }

    private UUID extractPayoutId(String payload) {
        // payload: {"payoutId":"uuid","recipientId":"...","amount":...,"currency":"..."}
        String marker = "\"payoutId\":\"";
        int start = payload.indexOf(marker) + marker.length();
        int end = payload.indexOf("\"", start);
        return UUID.fromString(payload.substring(start, end));
    }
}