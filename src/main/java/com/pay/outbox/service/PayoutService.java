package com.pay.outbox.service;

import com.pay.outbox.domain.entity.Payout;
import com.pay.outbox.domain.entity.OutboxEvent;
import com.pay.outbox.domain.enums.PayoutStatus;
import com.pay.outbox.dto.PayoutRequest;
import com.pay.outbox.dto.PayoutResponse;
import com.pay.outbox.repository.OutboxEventRepository;
import com.pay.outbox.repository.PayoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayoutService {

    private final PayoutRepository payoutRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public PayoutResponse initiatePayout(PayoutRequest request) {

        // check idempotency
        Optional<Payout> existing = payoutRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info("Duplicate request detected for idempotency key: {}", request.getIdempotencyKey());
            return mapToResponse(existing.get());
        }

        // create payout
        Payout payout = Payout.builder()
                .recipientId(request.getRecipientId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .idempotencyKey(request.getIdempotencyKey())
                .status(PayoutStatus.PENDING)
                .build();

        payoutRepository.save(payout);
        log.info("Payout created with id: {}", payout.getId());

        // create outbox event atomically in same transaction
        OutboxEvent event = OutboxEvent.builder()
                .payoutId(payout.getId())
                .eventType("PAYOUT_INITIATED")
                .payload(buildPayload(payout))
                .status("PENDING")
                .build();

        outboxEventRepository.save(event);
        log.info("Outbox event created for payout id: {}", payout.getId());

        return mapToResponse(payout);
    }

    @Transactional(readOnly = true)
    public PayoutResponse getPayoutById(UUID id) {
        Payout payout = payoutRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payout not found with id: " + id));
        return mapToResponse(payout);
    }

    private PayoutResponse mapToResponse(Payout payout) {
        return PayoutResponse.builder()
                .id(payout.getId())
                .recipientId(payout.getRecipientId())
                .amount(payout.getAmount())
                .currency(payout.getCurrency())
                .status(payout.getStatus())
                .createdAt(payout.getCreatedAt())
                .updatedAt(payout.getUpdatedAt())
                .attempts(payout.getAttempts())
                .build();
    }

    private String buildPayload(Payout payout) {
        return String.format(
                "{\"payoutId\":\"%s\",\"recipientId\":\"%s\",\"amount\":%d,\"currency\":\"%s\"}",
                payout.getId(), payout.getRecipientId(), payout.getAmount(), payout.getCurrency()
        );
    }
}