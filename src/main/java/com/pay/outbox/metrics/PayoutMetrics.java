package com.pay.outbox.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PayoutMetrics {

    private final Counter successCounter;
    private final Counter failedCounter;
    private final Counter hangCounter;
    private final Counter initiatedCounter;

    public PayoutMetrics(MeterRegistry registry) {
        this.initiatedCounter = Counter.builder("payout.initiated")
                .description("Total number of payouts initiated")
                .register(registry);

        this.successCounter = Counter.builder("payout.completed")
                .description("Total number of payouts completed successfully")
                .register(registry);

        this.failedCounter = Counter.builder("payout.failed")
                .description("Total number of payouts failed")
                .register(registry);

        this.hangCounter = Counter.builder("payout.hang")
                .description("Total number of payouts that hung and were retried")
                .register(registry);
    }

    public void incrementInitiated() {
        initiatedCounter.increment();
    }
    public void incrementCompleted() {
        successCounter.increment();
    }
    public void incrementFailed() {
        failedCounter.increment();
    }
    public void incrementHang() {
        hangCounter.increment();
    }
}