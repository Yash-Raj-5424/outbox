package com.pay.outbox.worker;

import com.pay.outbox.domain.entity.Payout;
import com.pay.outbox.domain.enums.PayoutStatus;
import com.pay.outbox.exception.PaymentExhaustedException;
import com.pay.outbox.metrics.PayoutMetrics;
import com.pay.outbox.repository.PayoutRepository;
import com.pay.outbox.service.LedgerService;
import com.pay.outbox.service.PaymentGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
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
    private static final String REDIS_DLQ_KEY = "payout:dlq";

    private final RedisTemplate<String, String> redisTemplate;
    private final PayoutRepository payoutRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final PayoutMetrics payoutMetrics;
    private final LedgerService ledgerService;


    @Scheduled(fixedDelay = 3000)
    @Async("paymentWorkerExecutor")
    @Transactional
    public void process() {
        // Pop from Redis queue (blocking left pop)
        String payload = redisTemplate.opsForList().leftPop(REDIS_QUEUE_KEY);

        if (payload == null){
            log.debug("No events in Redis queue");
            return;
        }
        log.info("Processing payload from queue: {}", payload);

        try{
            UUID payoutId = extractPayoutId(payload);
            Optional<Payout> optionalPayout = payoutRepository.findById(payoutId);

            if (optionalPayout.isEmpty()){
                log.debug("Payout not found for id: {}", payoutId);
                return;
            }

            Payout payout = optionalPayout.get();
            payout.setAttempts(payout.getAttempts() + 1);

            // simulate payment gateway response
            try {
                PayoutStatus result = paymentGatewayService.processPayment(payload);

                payout.setStatus(result);
                payoutRepository.save(payout);

                if (result == PayoutStatus.FAILED || result == PayoutStatus.CANCELLED) {
                    ledgerService.releaseBalance(payout.getRecipientId(), payout.getAmount());
                }

                switch (result) {
                    case COMPLETED -> payoutMetrics.incrementCompleted();
                    case FAILED -> payoutMetrics.incrementFailed();
                    default -> {
                    }
                }
                log.info("Payout id: {} processed with status: {}", payoutId, result);
            }catch(PaymentExhaustedException e){
                log.error("Payment exhausted for payout id: {}. Pushing to DLQ", payoutId);
                payout.setStatus(PayoutStatus.FAILED);
                payoutRepository.save(payout);
                ledgerService.releaseBalance(payout.getRecipientId(), payout.getAmount());
                redisTemplate.opsForList().rightPush(REDIS_DLQ_KEY, payload);
                payoutMetrics.incrementFailed();
                log.info("Pushed to DLQ: {}", payload);
            }

        } catch(Exception e){
            log.error("Error processing payment from queue", e);
        }
    }

    private UUID extractPayoutId(String payload) {

        String marker = "\"payoutId\":\"";
        int start = payload.indexOf(marker) + marker.length();
        int end = payload.indexOf("\"", start);
        return UUID.fromString(payload.substring(start, end));
    }
}