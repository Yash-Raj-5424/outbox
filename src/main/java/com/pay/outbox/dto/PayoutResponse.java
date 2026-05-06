package com.pay.outbox.dto;

import com.pay.outbox.domain.enums.PayoutStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PayoutResponse {

    private UUID id;
    private String recipientId;
    private Long amount;
    private String currency;
    private PayoutStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer attempts;
}