package com.pay.outbox.domain.enums;

public enum PayoutStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}
