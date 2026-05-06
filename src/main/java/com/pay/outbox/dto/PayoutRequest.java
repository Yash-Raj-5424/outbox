package com.pay.outbox.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PayoutRequest {

    @NotBlank(message = "Recipient ID is required")
    private String recipientId;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be greater than 0")
    private Long amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}