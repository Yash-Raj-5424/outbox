package com.pay.outbox.service;

import com.pay.outbox.domain.enums.PayoutStatus;
import com.pay.outbox.exception.PaymentHangException;
import com.pay.outbox.metrics.PayoutMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentGatewayService {

    private final Random random = new Random();
    private final PayoutMetrics payoutMetrics;

    @Retryable(
            retryFor = PaymentHangException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public PayoutStatus processPayment(String payload) {
        log.info("Calling payment gateway...");

        int outcome = random.nextInt(100);

        if (outcome < 70) {
            log.info("Payment gateway returned SUCCESS");
            return PayoutStatus.COMPLETED;
        } else if (outcome < 90) {
            log.warn("Payment gateway returned FAILED");
            return PayoutStatus.FAILED;
        } else {
            log.warn("Payment gateway HANG — will retry with backoff");
            payoutMetrics.incrementHang();
            throw new PaymentHangException("Payment gateway hung");
        }
    }

    @Recover
    public PayoutStatus recover(PaymentHangException e, String payload) {
        log.error("All retry attempts exhausted for payload: {}", payload);
        return PayoutStatus.FAILED;
    }
}