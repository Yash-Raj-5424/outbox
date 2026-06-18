package com.pay.outbox.service;

import com.pay.outbox.BaseIntegrationTest;
import com.pay.outbox.domain.entity.OutboxEvent;
import com.pay.outbox.domain.entity.Payout;
import com.pay.outbox.domain.enums.OutboxStatus;
import com.pay.outbox.domain.enums.PayoutStatus;
import com.pay.outbox.dto.PayoutRequest;
import com.pay.outbox.dto.PayoutResponse;
import com.pay.outbox.exception.PayoutNotFoundException;
import com.pay.outbox.repository.OutboxEventRepository;
import com.pay.outbox.repository.PayoutRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class PayoutServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PayoutService payoutService;

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private LedgerService ledgerService;

    @BeforeEach
    void setUp(){
        outboxEventRepository.deleteAll();
        payoutRepository.deleteAll();
        ledgerService.seedBalance("test_user", 1000000L);
    }

    @Test
    void shouldCreatePayoutAndOutboxEventInSingleTransaction(){
        PayoutRequest request = new PayoutRequest();
        request.setRecipientId("test_user");
        request.setAmount(50000L);
        request.setCurrency("INR");
        request.setIdempotencyKey("test-idempotency-001");

        PayoutResponse response = payoutService.initiatePayout(request);

        assertThat(request).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PayoutStatus.PENDING);
        assertThat(response.getAmount()).isEqualTo(50000L);

        //verify payout saved
        Optional<Payout> payout = payoutRepository.findById(response.getId());
        assertThat(payout).isPresent();
        assertThat(payout.get().getIdempotencyKey()).isEqualTo("test-idempotency-001");

        //verify outbox event saved
        List<OutboxEvent> outboxEvents = outboxEventRepository.findByStatus(OutboxStatus.PENDING);
        assertThat(outboxEvents).hasSize(1);
        assertThat(outboxEvents.get(0).getPayoutId()).isEqualTo(response.getId());
        assertThat(outboxEvents.get(0).getEventType()).isEqualTo("PAYOUT_INITIATED");
    }

    @Test
    void shouldReturnExistingPayoutOnDuplicateIdempotencyKey() {
        PayoutRequest request = new PayoutRequest();
        request.setRecipientId("test_user");
        request.setAmount(5000L);
        request.setCurrency("INR");
        request.setIdempotencyKey("test-idempotency-002");

        PayoutResponse firstResponse = payoutService.initiatePayout(request);
        PayoutResponse secondResponse = payoutService.initiatePayout(request);

        assertThat(firstResponse.getId()).isEqualTo(secondResponse.getId());
        assertThat(payoutRepository.count()).isEqualTo(1);
        assertThat(outboxEventRepository.count()).isEqualTo(1);
    }

    @Test
    void shuldThrowWhenPayoutNotFound(){
        assertThatThrownBy(() -> payoutService.getPayoutById(UUID.randomUUID()))
                .isInstanceOf(PayoutNotFoundException.class);

    }
}
